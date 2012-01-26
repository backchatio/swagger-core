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
    assert(doc.getApis.size === 2)
    assert((doc.getApis.map(api => api.getPath).toSet &
      Set("/pet.{format}",
        "/user.{format}")).size == 2)
  }

  it should "read the pet api description" in {
    val json = Source.fromURL("http://localhost:8002/api/pet.json").mkString
    val doc = JsonUtil.getJsonMapper.readValue(json, classOf[Documentation])
    assert(doc.getApis.size === 3)
    assert((doc.getApis.map(api => api.getPath).toSet &
      Set("/pet.{format}/{petId}",
        "/pet.{format}/findByStatus",
        "/pet.{format}/findByTags")).size == 3)
  }

  it should "read the user api with array and list data types as post data" in {
    val json = Source.fromURL("http://localhost:8002/api/user.json").mkString
    val doc = JsonUtil.getJsonMapper.readValue(json, classOf[Documentation])
    assert(doc.getApis.size === 6)
    assert((doc.getApis.map(api => api.getPath).toSet &
      Set("/user.{format}",
        "/user.{format}/createWithArray",
        "/user.{format}/createWithList")).size == 3)

    var param = doc.getApis.filter(api => api.getPath == "/user.{format}/createWithArray")(0).getOperations()(0).getParameters()(0)
    assert(param.getDataType() === "Array[User]")

    param = doc.getApis.filter(api => api.getPath == "/user.{format}/createWithList")(0).getOperations()(0).getParameters()(0)
    assert(param.getDataType() === "List[user]")

  }

}
