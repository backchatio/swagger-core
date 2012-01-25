package com.wordnik.test.swagger.jaxrs

import com.wordnik.swagger.core._
import com.wordnik.swagger.core.util._
import com.wordnik.swagger.jaxrs._

import com.wordnik.test.swagger.core.testdata._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

@RunWith(classOf[JUnitRunner])
class ResourceReaderTest extends FlatSpec with ShouldMatchers {
  "ApiPropertiesReader" should "read a SimplePojo" in {
    var docObj = ApiPropertiesReader.read(classOf[SampleOutput])
    assert(docObj != null)
    assert((docObj.getFields.map(f => f.name).toSet & Set("theName", "theValue")).size === 2)
    assert(docObj.getFields.filter(f => f.name == "id")(0).required == true)
  }

  behavior of "ApiReader"

  it should "read a simple resource class" in {
    val helpApi = new HelpApi
    // unclear to why we have "resourcePath" of /sample in this, should be detected from the Api object
    val doc = helpApi.filterDocs(ApiReader.read(classOf[BasicResourceJSON], "1.123", "2.345", "http://my.host.com/basepath", "/sample"),
      null,
      null,
      null)
    assert(doc.apiVersion == "1.123")
    assert(doc.swaggerVersion == "2.345")
    assert(doc.basePath == "http://my.host.com/basepath")
    assert(doc.resourcePath == "/sample")
    assert(doc.getApis.size == 1)
    assert(doc.getModels.size == 1)
    
    // verify the "howdy" model
    val props = doc.getModels().get("Howdy").properties.toMap
    assert((props.map(key=>key._1).toSet & Set("id", "theName", "theValue")).size == 3)
  }

  it should "read a resource class from a listing path" in {
    // simulate loading from the listing class
    val loadingClass = classOf[RemappedResourceJSON]
    val helpApi = new HelpApi
    // unclear to why we have "resourcePath" of /sample in this, should be detected from the Api object
    val doc = helpApi.filterDocs(ApiReader.read(loadingClass, "1.123", "2.345", "http://my.host.com/basepath", "/sample"),
      null,
      null,
      null)
    assert(doc.apiVersion === "1.123")
    assert(doc.swaggerVersion === "2.345")
    assert(doc.basePath === "http://my.host.com/basepath")
    assert(doc.resourcePath === "/sample")
    assert(doc.getApis.size === 1)
    assert(doc.getModels.size === 1)

    val props = doc.getModels().get("Howdy").properties.toMap
    assert((props.map(key=>key._1).toSet & Set("id", "theName", "theValue")).size == 3)
  }

  it should "NOT read a resource class from a listing path" in {
    // simulate loading from the listing class
    val loadingClass = classOf[RemappedResourceListingJSON]
    val helpApi = new HelpApi
    // unclear to why we have "resourcePath" of /sample in this, should be detected from the Api object
    val doc = helpApi.filterDocs(ApiReader.read(loadingClass, "1.123", "2.345", "http://my.host.com/basepath", "/sample"),
      null,
      null,
      null)
    assert(doc.apiVersion === "1.123")
    assert(doc.swaggerVersion === "2.345")
    assert(doc.basePath === "http://my.host.com/basepath")
    assert(doc.resourcePath === "/sample")
    assert(doc.getApis === null)
  }
}
