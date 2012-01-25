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

package com.wordnik.swagger.jaxrs

import com.wordnik.swagger.core._
import com.wordnik.swagger.core.ApiValues._
import com.wordnik.swagger.core.util.TypeUtil

import org.slf4j.LoggerFactory

import javax.ws.rs._
import core.Context
import util.ReflectionUtil

import java.lang.reflect.{Type, Field, Modifier, Method}
import java.lang.annotation.Annotation
import javax.xml.bind.annotation._

import scala.collection.JavaConversions._
import collection.mutable.ListBuffer

object ApiReader {
  private val LOGGER = LoggerFactory.getLogger(ApiReader.getClass)

  val GET = "GET"
  val PUT = "PUT"
  val DELETE = "DELETE"
  val POST = "POST"
  val HEAD = "HEAD"

  val FORMAT_STRING = ".{format}"
  val LIST_RESOURCES_PATH = "/resources"

  private val endpointsCache = scala.collection.mutable.Map.empty[Class[_], Documentation]

  def read(hostClass: Class[_], apiVersion: String, swaggerVersion: String, basePath: String, apiPath: String): Documentation = {
    LOGGER.debug("reading path " + apiPath)

    endpointsCache.get(hostClass) match {
      case None => {
        val doc = new ApiSpecParser(hostClass, apiVersion, swaggerVersion, basePath, apiPath).parse
        endpointsCache += hostClass -> doc.clone.asInstanceOf[Documentation]
        doc
      }
      case doc: Option[Documentation] => doc.get.clone.asInstanceOf[Documentation]
      case _ => null
    }
  }
}

class ApiSpecParser(val hostClass: Class[_], val apiVersion: String, val swaggerVersion: String, val basePath: String, val resourcePath: String) extends BaseApiParser {
  private val LOGGER = LoggerFactory.getLogger(classOf[ApiSpecParser])
  private val TRAIT = "trait"

  LOGGER.debug(hostClass + ", apiVersion: " + apiVersion + ", swaggerVersion: " + swaggerVersion + ", basePath: " + basePath + ", resourcePath: " + resourcePath)

  val documentation = new Documentation

  val apiEndpoint = hostClass.getAnnotation(classOf[Api])

  def parse(): Documentation = {
    if (apiEndpoint != null) {
      for (method <- hostClass.getMethods) parseMethod(method)
    }
    documentation.apiVersion = apiVersion
    documentation.swaggerVersion = swaggerVersion
    documentation.basePath = basePath
    documentation.resourcePath = resourcePath
    documentation
  }

  private def parseHttpMethod(method: Method, apiOperation: ApiOperation): String = {
    if (apiOperation.httpMethod() != null && apiOperation.httpMethod().trim().length() > 0)
      apiOperation.httpMethod().trim()
    else {
      val wsGet = method.getAnnotation(classOf[javax.ws.rs.GET])
      val wsDelete = method.getAnnotation(classOf[javax.ws.rs.DELETE])
      val wsPost = method.getAnnotation(classOf[javax.ws.rs.POST])
      val wsPut = method.getAnnotation(classOf[javax.ws.rs.PUT])
      val wsHead = method.getAnnotation(classOf[javax.ws.rs.HEAD])

      if (wsGet != null) ApiReader.GET
      else if (wsDelete != null) ApiReader.DELETE
      else if (wsPost != null) ApiReader.POST
      else if (wsPut != null) ApiReader.PUT
      else if (wsHead != null) ApiReader.HEAD
      else null
    }
  }

  private def parseApiParam(docParam: DocumentationParameter, apiParam: ApiParam, method: Method) {
    docParam.name = readString(apiParam.name, docParam.name)
    docParam.description = readString(apiParam.value)
    docParam.defaultValue = readString(apiParam.defaultValue)
    try {
      docParam.allowableValues = convertToAllowableValues(apiParam.allowableValues)
    } catch {
      case e: RuntimeException =>
        LOGGER.error("Allowable values annotation is wrong in method  " + method +
          "for parameter " + docParam.name)
        e.printStackTrace()
    }
    docParam.required = apiParam.required
    docParam.allowMultiple = apiParam.allowMultiple
    docParam.paramAccess = readString(apiParam.access)
  }

  private def parseMethod(method: Method): Any = {
    val apiOperation = method.getAnnotation(classOf[ApiOperation])
    val apiErrors = method.getAnnotation(classOf[ApiErrors])
    val isDeprecated = method.getAnnotation(classOf[Deprecated])

    if (apiOperation != null && method.getName != "getHelp") {

      // Read the Operation
      val docOperation = new DocumentationOperation

      // check if its deprecated
      if (isDeprecated != null) docOperation.deprecated = true

      if (apiOperation != null) {
        docOperation.httpMethod = parseHttpMethod(method, apiOperation)
        docOperation.summary = readString(apiOperation.value)
        docOperation.notes = readString(apiOperation.notes)
        docOperation.setTags(toObjectList(apiOperation.tags))
        docOperation.nickname = method.getName
        val apiResponseValue = readString(apiOperation.responseClass)
        val isResponseMultiValue = apiOperation.multiValueResponse

        docOperation.setResponseTypeInternal(apiResponseValue)
        try {
          val clazz = SwaggerContext.loadClass(apiResponseValue)
          val annotatedName = ApiPropertiesReader.readName(clazz)
          docOperation.responseClass = if (isResponseMultiValue) "List[" + annotatedName + "]" else annotatedName
        } catch {
          case e: ClassNotFoundException => docOperation.responseClass = apiResponseValue
        }
      }

      // Read method annotations for implicit api params which are not declared as actual argments to the method
      // Essentially ApiParamImplicit annotations on method
      val methodAnnotations = method.getAnnotations
      for (ma <- methodAnnotations) {
        ma match {
          case pSet: ApiParamsImplicit => {
            for (p <- pSet.value()) {
              val docParam = new DocumentationParameter
              docParam.paramType = TYPE_QUERY

              docParam.name = readString(p.name)
              docParam.description = readString(p.value)
              docParam.defaultValue = readString(p.defaultValue)
              try {
                docParam.allowableValues = convertToAllowableValues(p.allowableValues)
              } catch {
                case e: RuntimeException =>
                  LOGGER.error("Allowable values annotation is wrong in method  " + method +
                    "for parameter " + docParam.name)
                  e.printStackTrace()
              }
              docParam.required = p.required
              docParam.allowMultiple = p.allowMultiple
              docParam.paramAccess = readString(p.access)
              docParam.internalDescription = readString(p.internalDescription)
              docParam.dataType = readString(p.dataType)
              docParam.paramType = readString(p.paramType)
              docParam.paramType = if (docParam.paramType == null) TYPE_QUERY else docParam.paramType

              docOperation.addParameter(docParam)
            }
          }
          case _ => Unit
        }
      }

      // Read the params and add to Operation
      val paramAnnotationDoubleArray = method.getParameterAnnotations
      val paramTypes = method.getParameterTypes
      var counter = 0
      var ignoreParam = false
      for (paramAnnotations <- paramAnnotationDoubleArray) {
        val docParam = new DocumentationParameter
        docParam.required = true

        // determine value type
        try {
          val paramTypeClass = paramTypes(counter)
          val paramTypeName = ApiPropertiesReader.readName(paramTypeClass)
          docParam.dataType = paramTypeName
          if (!paramTypeClass.isPrimitive && !paramTypeClass.getName().contains("java.lang")) {
            docParam.setValueTypeInternal(paramTypeClass.getName)
          }
        } catch {
          case e: Exception => LOGGER.error("Unable to determine datatype for param " + counter + " in method " + method, e)
        }

        for (pa <- paramAnnotations) {
          ignoreParam = false
          pa match {
            case apiParam: ApiParam => parseApiParam(docParam, apiParam, method)
            case wsParam: QueryParam => {
              docParam.name = readString(wsParam.value, docParam.name)
              docParam.paramType = readString(TYPE_QUERY, docParam.paramType)
            }
            case wsParam: PathParam => {
              docParam.name = readString(wsParam.value, docParam.name)
              docParam.required = true
              docParam.paramType = readString(TYPE_PATH, docParam.paramType)
            }
            case wsParam: MatrixParam => {
              docParam.name = readString(wsParam.value, docParam.name)
              docParam.paramType = readString(TYPE_MATRIX, docParam.paramType)
            }
            case wsParam: HeaderParam => {
              docParam.name = readString(wsParam.value, docParam.name)
              docParam.paramType = readString(TYPE_HEADER, docParam.paramType)
            }
            case wsParam: FormParam => {
              docParam.name = readString(wsParam.value, docParam.name)
              docParam.paramType = readString(TYPE_FORM, docParam.paramType)
            }
            case wsParam: CookieParam => {
              docParam.name = readString(wsParam.value, docParam.name)
              docParam.paramType = readString(TYPE_COOKIE, docParam.paramType)
            }
            case wsParam: Context => ignoreParam = true
            case _ => Unit
          }
        }

        if (paramAnnotations.length == 0) {
          ignoreParam = true
        }

        counter = counter + 1

        // Set the default paramType, if nothing is assigned
        docParam.paramType = readString(TYPE_BODY, docParam.paramType)
        if (!ignoreParam) docOperation.addParameter(docParam)
      }

      // Get Endpoint
      val docEndpoint = getEndPoint(documentation, getPath(method))

      // Add Operation to Endpoint
      docEndpoint.addOperation(processOperation(method, docOperation))

      // Read the Errors and add to Response
      if (apiErrors != null) {
        for (apiError <- apiErrors.value) {
          val docError = new DocumentationError
          docError.code = apiError.code
          docError.reason = readString(apiError.reason)
          docOperation.addErrorResponse(docError)
        }
      }
    }
  }

  protected def processOperation(method: Method, o: DocumentationOperation) = o

  protected def getPath(method: Method): String = {
    val wsPath = method.getAnnotation(classOf[javax.ws.rs.Path])
    val path = apiEndpoint.value + ApiReader.FORMAT_STRING + (if (wsPath == null) "" else wsPath.value)
    path
  }

  private def getCategory(method: Method): String = {
    val declaringInterface = ReflectionUtil.getDeclaringInterface(method)
    if (declaringInterface == null)
      null
    else {
      val simpleName = declaringInterface.getSimpleName
      if (simpleName.toLowerCase.endsWith(TRAIT) && simpleName.length > TRAIT.length)
        simpleName.substring(0, simpleName.length - TRAIT.length)
      else
        simpleName
    }
  }

  private def getEndPoint(documentation: Documentation, path: String): DocumentationEndPoint = {
    var ep: DocumentationEndPoint = null

    if (documentation.getApis != null)
      for (endpoint <- asIterable(documentation.getApis)) {
        if (endpoint.path == path) ep = endpoint
      }

    ep match {
      case null => ep = new DocumentationEndPoint(path, apiEndpoint.description); documentation.addApi(ep); ep
      case _ => ep
    }
  }
}