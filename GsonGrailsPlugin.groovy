import grails.plugin.gson.adapters.GrailsDomainDeserializer
import grails.plugin.gson.adapters.GrailsDomainSerializer
import grails.plugin.gson.converters.GsonParsingParameterCreationListener
import grails.plugin.gson.spring.GsonBuilderFactory
import grails.plugin.gson.support.proxy.DefaultEntityProxyHandler
import grails.plugin.gson.support.proxy.ProxyHandlerFacade

class GsonGrailsPlugin {

  def version = '2.1'
  def grailsVersion = '2.4 > *'
  def dependsOn = [:]
  def loadAfter = ['controllers', 'converters']
  def pluginExcludes = [
    'grails-app/views/**/*'
  ]

  def title = 'Gson Plugin'
  def author = 'Rob Fletcher'
  def authorEmail = 'rob@freeside.co'
  def description = 'Provides alternate JSON (de)serialization using Google\'s Gson library'
  def documentation = 'http://git.io/grails-gson'
  def license = 'APACHE'
  def organization = [name: 'Freeside Software', url: 'http://freeside.co']
  def issueManagement = [system: 'GitHub', url: 'https://github.com/robfletcher/grails-gson/issues']
  def scm = [url: 'https://github.com/robfletcher/grails-gson']

  def doWithSpring = {
    if (!(manager?.hasGrailsPlugin('hibernate') || manager?.hasGrailsPlugin('hibernate4'))) {
      proxyHandler DefaultEntityProxyHandler
    }
    domainSerializer (GrailsDomainSerializer) {bean ->
      bean.autowire = "byName"
    }
    domainDeserializer (GrailsDomainDeserializer) {bean ->
      bean.autowire = "byName"
    }
    gsonBuilder (GsonBuilderFactory) {bean ->
      bean.autowire = "byName"
    }
    jsonParsingParameterCreationListener (GsonParsingParameterCreationListener){ bean->
      bean.autowire = "byName"
    }
  }

  def doWithDynamicMethods = { ctx ->
    def enhancer = new grails.plugin.gson.api.ArtefactEnhancer(application, ctx.gsonBuilder, ctx.domainDeserializer)
    enhancer.enhanceRequest()
    enhancer.enhanceControllers()
    enhancer.enhanceDomains()
  }

}
