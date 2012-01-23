package com.wordnik.test.swagger.core.testdata

import com.wordnik.swagger.core._
import com.wordnik.swagger.core.ApiError
import com.wordnik.swagger.core.ApiParam._
import com.wordnik.swagger.core.ApiOperation._

import javax.ws.rs._
import javax.ws.rs.core.Response

class SampleResource {
  @GET
  @ApiOperation(value = "Get object by ID",
    notes = "No details provided",
    responseClass = "com.wordnik.test.swagger.core.testdata.SampleOutput")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid ID"),
    new ApiError(code = 404, reason = "object not found")))
  def getTest(
    @ApiParam(value = "sample param data", required = true, allowableValues = "range[0,10]")@QueryParam("id") id: String) = {
    val out = new SampleOutput
    out.name = "foo"
    out.value = "bar"
    Response.ok.entity(out).build
  }
}
