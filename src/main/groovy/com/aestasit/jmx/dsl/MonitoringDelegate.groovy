package com.aestasit.jmx.dsl


import groovy.util.GroovyMBean;
import javax.management.ObjectName 

abstract class MonitoringDelegate implements Runnable {
  
  def refreshRate = 1000
  
  protected def MonitoringContext context      = null
  protected def modules                        = []
  
  MonitoringDelegate(modules, context) {
    this.modules = modules
    this.context = context
  }
  
  public void run() {
    while (true) {
      try {
        poll()
      } catch (Throwable e) {
        e.printStackTrace();
      }
      sleep(refreshRate)
    }
  }
  
  abstract def void poll()
  
  def Collection getValues(GroovyMBean m, Collection attributes) {
    
    // println "Processing attributes of '" + m.name() + "'"
    def Collection beanAttrs = m.listAttributeNames()
    def Hashtable nameProps = m.name().getKeyPropertyList()
    
    // Gather mbean attribute values.
    def values = []
    attributes.each { attr->
      if (beanAttrs.contains(attr)) {
        def value = m."$attr"
        values.add(value)
      } else if (nameProps.containsKey(attr)) {
        def value = m.name().getKeyProperty(attr)
        values.add(value)
      } else {
        println "Attribute '$attr' not found!"
        values.add(0L)
      }
    }
    
    return values
  }
  
  
  private def serverNameCache = [:]
  
  def getServerName(GroovyMBean m) {
    def serverHash = m.server().toString()
    if (!serverNameCache.containsKey(serverHash)) {
      println "Calculating server name for: " + serverHash
      try {
        def serverName = m.server().getAttribute(new ObjectName("com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean"), "ServerName")
        serverNameCache[serverHash] = serverName
      } catch (Exception e) {
        def args =  m.server().getAttribute(new ObjectName("java.lang:type=Runtime"), "InputArguments")
        serverNameCache[serverHash] = serverHash
        args.each { String arg ->
          if (arg.startsWith("-Dweblogic.Name=")) {
            serverNameCache[serverHash] = arg.substring("-Dweblogic.Name=".length())
          }
        }
      }
    }
    return serverNameCache[serverHash]
  }
  
  private def singleServer = null
  
  def isSingleServerContext() {
    if (singleServer == null) {
      def hashCodes = []as Set
      modules.each { GroovyMBean m ->
        hashCodes.add(m.server().toString())
      }
      singleServer = (hashCodes.size() == 1)
    }
    return singleServer
  }
}
