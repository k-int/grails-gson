package grails.plugin.gson.adapters

import groovy.util.logging.Slf4j

import java.lang.reflect.Type

import org.apache.commons.beanutils.PropertyUtils
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.springframework.util.ReflectionUtils

import com.google.gson.*

@Slf4j
class GrailsDomainSerializer<T> implements JsonSerializer<T> {

  GrailsApplication grailsApplication
  ProxyHandler proxyHandler

  private final Stack<GrailsDomainClassProperty> circularityStack = new Stack<GrailsDomainClassProperty>()

  @Override
  JsonElement serialize(T instance, Type type, JsonSerializationContext context) {
    def element = new JsonObject()
    if (shouldOutputClass()) {
      element.add 'class', context.serialize(instance.getClass().name)
    }
    eachUnvisitedProperty(instance) { GrailsDomainClassProperty property ->
      def field = ReflectionUtils.findField(instance.getClass(), property.name)
      if (!field) {
        throw new NoSuchFieldException(property.name)
      }
      def value = PropertyUtils.getProperty(instance, property.name)
      def elementName = fieldNamingStrategy.translateName(field)
      if (proxyHandler.isProxy(value)) {
        if (shouldSerializeProxy() && (shouldResolveProxy() || proxyHandler.isInitialized(value))) {
          log.debug "unwrapping proxy for $property.domainClass.shortName.$property.name"
          value = proxyHandler.unwrapIfProxy(value)
          element.add elementName, context.serialize(value, property.type)
        } else if (property.oneToMany || property.manyToMany) {
          log.debug "skipping proxied collection $property.domainClass.shortName.$property.name"
        } else {
          log.debug "not unwrapping proxy for $property.domainClass.shortName.$property.name"
          value = [id: proxyHandler.getProxyIdentifier(value)]
          element.add elementName, context.serialize(value, Map)
        }
      } else {
        element.add elementName, context.serialize(value, property.type)
      }
    }
    element
  }

  private void eachUnvisitedProperty(T instance, Closure iterator) {
    eachProperty(instance) { GrailsDomainClassProperty property ->
      if (property in circularityStack) {
        handleCircularReference property
      } else if (property.bidirectional) {
        circularityStack.push property.otherSide
        iterator property
        circularityStack.pop()
      } else {
        iterator property
      }
    }
  }

  private void eachProperty(T instance, Closure iterator) {
    def domainClass = getDomainClassFor(instance)
    iterator(domainClass.identifier)
    if (shouldOutputVersion()) {
      iterator(domainClass.version)
    }
    for (property in domainClass.persistentProperties) iterator(property)
  }

  private void handleCircularReference(GrailsDomainClassProperty property) {
    log.debug "already dealt with $property.domainClass.shortName.$property.name"
  }

  private GrailsDomainClass getDomainClassFor(T instance) {

    T unwrapped = proxyHandler.unwrapIfProxy(instance)

    log.debug ("looking for domain class for ${instance} using ${unwrapped.getClass().name}")
    // TODO: may need to cache this
    grailsApplication.getArtefact("Domain", "${unwrapped.getClass().name}")
  }

  @Lazy
  private FieldNamingStrategy fieldNamingStrategy = {
    grailsApplication.config.get('grails.converters.gson.fieldNamingPolicy', FieldNamingPolicy.IDENTITY)
  }()

  private boolean shouldResolveProxy() {
    
    grailsApplication.config.get('grails.converters.gson.resolveProxies', true)
  }

  private boolean shouldSerializeProxy() {
    grailsApplication.config.get('grails.converters.gson.serializeProxies', true)
  }

  private boolean shouldOutputClass() {
    grailsApplication.config.get('grails.converters.gson.domain.include.class', grailsApplication.config.get('grails.converters.domain.include.class', false))
  }

  private boolean shouldOutputVersion() {
    grailsApplication.config.get('grails.converters.gson.domain.include.version', grailsApplication.config.get('grails.converters.domain.include.version', false))
  }
}
