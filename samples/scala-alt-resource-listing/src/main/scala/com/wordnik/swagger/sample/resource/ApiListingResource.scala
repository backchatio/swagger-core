package com.wordnik.swagger.sample.resource

import com.wordnik.swagger.core._
import com.wordnik.swagger.jaxrs._

import javax.ws.rs.{Produces, Path}

@Path("/resources")
@Api("/resources")
@Produces(Array("application/json","application/xml"))
class ApiListingResourceJSONXML extends ApiListing
