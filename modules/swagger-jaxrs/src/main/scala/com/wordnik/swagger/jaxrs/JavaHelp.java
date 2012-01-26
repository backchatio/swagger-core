/**
 *  Copyright 2011 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.wordnik.swagger.jaxrs;

import java.io.IOException;
import java.lang.reflect.Constructor;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

import com.sun.jersey.api.core.ResourceConfig;
import com.wordnik.swagger.core.*;

public abstract class JavaHelp {
	@GET
	public Response getHelp(@Context ServletConfig servConfig,
			@Context ResourceConfig resConfig, @Context HttpHeaders headers,
			@Context UriInfo uriInfo) throws JsonGenerationException,
			JsonMappingException, IOException {
		ConfigReader configReader = ConfigReaderFactory
				.getConfigReader(servConfig);

		String apiVersion = configReader.getApiVersion();
		String swaggerVersion = configReader.getSwaggerVersion();
		String basePath = configReader.getBasePath();
		String apiFilterClassName = configReader.getApiFilterClassName();

		boolean filterOutTopLevelApi = true;
		Api currentApiEndPoint = this.getClass().getAnnotation(Api.class);

		if (currentApiEndPoint == null) {
			return Response.status(Status.NOT_FOUND).build();
		} else {
			String apiPath;

			if (filterOutTopLevelApi) {
				apiPath = currentApiEndPoint.value();
			} else
				apiPath = null;
			String apiListingPath;
			if (filterOutTopLevelApi) {
				if (!"".equals(currentApiEndPoint.listingPath()))
					apiListingPath = currentApiEndPoint.listingPath();
				else
					apiListingPath = currentApiEndPoint.value();
			} else
				apiListingPath = null;

			Class<?> listingClass = this.getClass();
			if (!"".equals(currentApiEndPoint.listingClass())) {
				listingClass = SwaggerContext.loadClass(currentApiEndPoint
						.listingClass());
			}
			HelpApi helpApi = new HelpApi(apiFilterClassName);
			Documentation docs = helpApi
					.filterDocs(ApiReader.read(listingClass, apiVersion,
							swaggerVersion, basePath, apiPath), headers,
							uriInfo, apiListingPath, apiPath);
			return Response.ok().entity(docs).build();
		}
		/*
		 * Documentation docs =
		 * helpApi.filterDocs(ApiReader.read(this.getClass(), apiVersion,
		 * swaggerVersion, basePath, currentApiPath), headers, uriInfo,
		 * currentApiPath);
		 * 
		 * 
		 * String currentApiPath = currentApiEndPoint != null &&
		 * filterOutTopLevelApi ? currentApiEndPoint.value() : null;
		 * 
		 * HelpApi helpApi = new HelpApi(apiFilterClassName);
		 * System.out.println(this.getClass()); Documentation docs =
		 * helpApi.filterDocs(ApiReader.read(this.getClass(), apiVersion,
		 * swaggerVersion, basePath, currentApiPath), headers, uriInfo,
		 * currentApiPath); Response response = Response.ok(docs).build();
		 * return response;
		 */
	}
}
