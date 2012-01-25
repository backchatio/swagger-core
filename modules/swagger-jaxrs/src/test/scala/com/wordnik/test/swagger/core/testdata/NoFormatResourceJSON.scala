package com.wordnik.test.swagger.core.testdata

import com.wordnik.swagger.core._
import com.wordnik.swagger.core.ApiParam._
import com.wordnik.swagger.core.ApiOperation._

import javax.ws.rs._
import javax.ws.rs.core.Response

// the actual resource
@Path("/basic")
@Api(value = "/basic", 
    description = "Basic resource", 
    listingPath="/resources/basic")
@Produces(Array("application/json"))
class NoFormatResourceJSON extends BasicResource

// listing resource which refers to the actual resource
@Path("/resources/basic")
@Api(value = "/basic", 
    description = "Basic resource", 
    listingPath="/resources/basic",
    listingClass="com.wordnik.test.swagger.core.testdata.NoFormatResourceJSON")
@Produces(Array("application/json"))
class NoFormatResourceListingJSON extends Help
