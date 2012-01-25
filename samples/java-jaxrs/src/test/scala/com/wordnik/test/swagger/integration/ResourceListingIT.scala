package com.wordnik.test.swagger.integration

import com.wordnik.swagger.core._
import com.wordnik.swagger.core.util.JsonUtil

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.JavaConversions._

import scala.io._

@RunWith(classOf[JUnitRunner])
class ResourceListingIT extends FlatSpec with ShouldMatchers {
  it should "read a resource listing" in {
    val json = Source.fromURL("http://localhost:8002/api/resources.json").mkString
    val doc = JsonUtil.getJsonMapper.readValue(json, classOf[Documentation])
    assert(doc.getApis.size == 2)
    assert((doc.getApis.map(api => api.getPath).toSet & Set("/pet.{format}", "/user.{format}")).size == 0)
  }
}
