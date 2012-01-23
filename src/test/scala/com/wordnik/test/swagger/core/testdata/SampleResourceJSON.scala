package com.wordnik.test.swagger.core.testdata

import com.wordnik.swagger.core._
import com.wordnik.swagger.core.ApiParam._
import com.wordnik.swagger.core.ApiOperation._

import javax.ws.rs._
import javax.ws.rs.core.Response


@Path("/sample.json")
@Api(value = "/sample", description = "Sample resource")
@Produces(Array("application/json"))
class SampleResourceJSON extends SampleResource 