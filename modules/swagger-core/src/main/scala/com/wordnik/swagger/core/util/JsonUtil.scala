package com.wordnik.swagger.core.util

import org.codehaus.jackson.map._
import org.codehaus.jackson.map.DeserializationConfig.Feature
import org.codehaus.jackson.map.annotate.JsonSerialize
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector

object JsonUtil {
  def getJsonMapper = {
    val jackson = new JacksonAnnotationIntrospector()
    val jaxb = new JaxbAnnotationIntrospector()
    val pair = new AnnotationIntrospector.Pair(jaxb, jackson)
    val mapper = new ObjectMapper()

    mapper.getSerializationConfig().setAnnotationIntrospector(jaxb);
    mapper.getDeserializationConfig().setAnnotationIntrospector(pair);
    mapper.getDeserializationConfig().set(Feature.AUTO_DETECT_SETTERS, true);
    mapper.configure(Feature.AUTO_DETECT_SETTERS, true);
    mapper.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);
    mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper
  }
}