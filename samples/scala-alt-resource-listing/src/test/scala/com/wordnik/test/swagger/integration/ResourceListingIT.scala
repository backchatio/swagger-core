package com.wordnik.test.swagger.integration

import com.wordnik.swagger.core._
import com.wordnik.swagger.core.util.JsonUtil

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.config.DefaultClientConfig

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.JavaConversions._

import scala.io._

@RunWith(classOf[JUnitRunner])
class ResourceListingIT extends FlatSpec with ShouldMatchers {
  it should "read a resource listing in JSON" in {
    val json = Client.create(new DefaultClientConfig()).resource("http://localhost:8002/api/resources").accept("application/json").get(classOf[String])
    val doc = JsonUtil.getJsonMapper.readValue(json, classOf[Documentation])
    assert(doc.getApis.size === 2)
    assert((doc.getApis.map(api => api.getPath).toSet & Set("/resources/pet", "/resources/user")).size == 2)
  }

  it should "read a resource listing in XML" in {
    val xmlString = Client.create(new DefaultClientConfig()).resource("http://localhost:8002/api/resources").accept("application/xml").get(classOf[String])
    val xml = scala.xml.XML.loadString(xmlString)
    assert(((xml \ "apis").map(api => (api \ "path").text).toSet & Set("/resources/pet", "/resources/user")).size == 2)
  }

  it should "read the pet api description in JSON" in {
    val json = Client.create(new DefaultClientConfig()).resource("http://localhost:8002/api/resources/pet").accept("application/json").get(classOf[String])
    val doc = JsonUtil.getJsonMapper.readValue(json, classOf[Documentation])
    assert(doc.getApis.size === 4)
    assert((doc.getApis.map(api => api.getPath).toSet &
      Set("/pet",
        "/pet/{petId}",
        "/pet/findByStatus",
        "/pet/findByTags")).size == 4)
  }
}
