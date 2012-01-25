package com.wordnik.test.swagger.core.testdata


import com.wordnik.swagger.core._
import com.wordnik.swagger.jaxrs._
import com.wordnik.swagger.core.ApiParam._
import com.wordnik.swagger.core.ApiOperation._

import javax.ws.rs._
import javax.ws.rs.core.Response

// the actual resource
@Path("/basic.json")
@Api(value = "/basic", 
    description = "Basic resource", 
    listingPath="/resources.json/basic")
@Produces(Array("application/json"))
class RemappedResourceJSON extends BasicResource

// listing resource which refers to the actual resource
@Path("/resources.json/basic")
@Api(value = "/basic", 
    description = "Basic resource", 
    listingPath="/resources.json/basic",
    listingClass="com.wordnik.test.swagger.core.testdata.RemappedResourceJSON")
@Produces(Array("application/json"))
class RemappedResourceListingJSON extends Help