package com.wordnik.test.swagger.core

import com.wordnik.swagger.core._

import com.wordnik.test.swagger.core.testdata._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

@RunWith(classOf[JUnitRunner])
class ResourceReaderTest extends FlatSpec with ShouldMatchers {
  behavior of "ApiReader"

  it should "read a SimplePojo" in {
    var docObj = ApiPropertiesReader.read(classOf[SampleOutput])
    assert(docObj != null)
    assert((docObj.getFields.map(f=>f.name).toSet & Set("theName","theValue")).size === 2)
  }

  it should "read a simple resource class" in {
    val helpApi = new HelpApi
    
    // unclear to why we have "resourcePath" of /sample in this
    val doc = helpApi.filterDocs(ApiReader.read(classOf[SampleResourceJSON], "1.123", "2.345", "http://my.host.com/basepath", "/sample"),
    		null,
    		null,
    		null)
    assert(doc.getApis.size == 1)
    assert(doc.getModels.size == 1)
    assert((doc.getModels(0).getFields.map(f=>f.name).toSet & Set("theName","theValue")).size === 2)
    println(JsonUtil.getJsonMapper.writeValueAsString(doc))
  }
}
