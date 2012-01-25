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

package com.wordnik.swagger.core

import com.wordnik.swagger.core.util.ReflectionUtil
import com.wordnik.swagger.core.ApiValues._
import com.wordnik.swagger.core.util.TypeUtil

import org.slf4j.LoggerFactory

import java.lang.reflect.{ Type, Field, Modifier, Method }
import java.lang.annotation.Annotation
import javax.xml.bind.annotation._

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl

object ApiPropertiesReader {
  private val modelsCache = scala.collection.mutable.Map.empty[Class[_], DocumentationObject]

  def read(hostClass: Class[_]): DocumentationObject = {
    modelsCache.get(hostClass) match {
      case None => val docObj = new ApiModelParser(hostClass).parse; modelsCache += hostClass -> docObj; docObj
      case docObj: Option[DocumentationObject] => docObj.get
      case _ => null
    }
  }

  def readName(hostClass: Class[_]): String = {
    new ApiModelParser(hostClass).readName(hostClass)
  }
}

private class ApiModelParser(val hostClass: Class[_]) extends BaseApiParser {
  private val documentationObject = new DocumentationObject
  private val LOGGER = LoggerFactory.getLogger(classOf[ApiModelParser])

  documentationObject.setName(readName(hostClass))

  private val xmlElementTypeMethod = classOf[XmlElement].getDeclaredMethod("type")

  def readName(hostClass: Class[_]): String = {
    val xmlRootElement = hostClass.getAnnotation(classOf[XmlRootElement])
    val xmlEnum = hostClass.getAnnotation(classOf[XmlEnum])

    if (xmlEnum != null && xmlEnum.value() != null) {
      readName(xmlEnum.value())
    } else if (xmlRootElement != null) {
      if ("##default".equals(xmlRootElement.name())) {
        hostClass.getSimpleName
      } else {
        readString(xmlRootElement.name())
      }
    } else if (hostClass.getName.startsWith("java.lang.")) {
      hostClass.getName.substring("java.lang.".length).toLowerCase
    } else if (hostClass.getName.indexOf(".") < 0) {
      hostClass.getName
    } else {
      LOGGER.error("Class " + hostClass.getName + " is not annotated with a @XmlRootElement annotation, using " + hostClass.getSimpleName)
      hostClass.getSimpleName
    }
  }

  def parse(): DocumentationObject = {
    for (method <- hostClass.getDeclaredMethods) {
      if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()))
        parseMethod(method)
    }

    for (field <- hostClass.getDeclaredFields) {
      if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
        parseField(field)
    }
    documentationObject
  }

  private def parseField(field: Field): Any = {
    parsePropertyAnnotations(field.getName, field.getAnnotations, field.getGenericType, field.getType)
  }

  private def parseMethod(method: Method): Any = {
    if (method.getParameterTypes == null || method.getParameterTypes.length == 0)
      parsePropertyAnnotations(method.getName, method.getAnnotations, method.getGenericReturnType, method.getReturnType)
  }

  private def extractGetterProperty(methodFieldName: String): String = {
    if (methodFieldName != null &&
      (methodFieldName.startsWith("get")) &&
      methodFieldName.length > 3) {
      methodFieldName.substring(3, 4).toLowerCase() + methodFieldName.substring(4, methodFieldName.length())
    } else if (methodFieldName != null &&
      (methodFieldName.startsWith("is")) &&
      methodFieldName.length > 2) {
      methodFieldName.substring(2, 3).toLowerCase() + methodFieldName.substring(3, methodFieldName.length())
    } else {
      methodFieldName
    }
  }

  def parsePropertyAnnotations(methodFieldName: String, methodAnnotations: Array[Annotation], genericReturnType: Type, returnType: Type): Any = {
    val name = extractGetterProperty(methodFieldName)

    val docParam = new DocumentationParameter
    docParam.required = false
    var isTransient = false

    isTransient = processAnnotations(name, methodAnnotations, docParam)

    if(!isTransient && null != name){
      try {
        val propertyAnnotations = this.hostClass.getDeclaredField(name).getAnnotations()
        isTransient = processAnnotations(name, propertyAnnotations, docParam)
      } catch {
        //this means there is no field declared to look for field level annotations.
        case e: java.lang.NoSuchFieldException => isTransient = false
      }
    }

    if (docParam.name == null && name != null)
      docParam.name = name

    if (!isTransient && docParam.name != null) {
      if (docParam.paramType == null) {
        if (TypeUtil.isParameterizedList(genericReturnType)) {
          val parameterizedType = genericReturnType.asInstanceOf[java.lang.reflect.ParameterizedType]
          val valueType = parameterizedType.getActualTypeArguments.head
          docParam.paramType = "List[" + readName(valueType.asInstanceOf[Class[_]]) + "]"
        } else if (TypeUtil.isParameterizedSet(genericReturnType)) {
          val parameterizedType = genericReturnType.asInstanceOf[java.lang.reflect.ParameterizedType]
          val valueType = parameterizedType.getActualTypeArguments.head
          docParam.paramType = "Set[" + readName(valueType.asInstanceOf[Class[_]]) + "]"
        } else if (TypeUtil.isParameterizedMap(genericReturnType)) {
          val parameterizedType = genericReturnType.asInstanceOf[java.lang.reflect.ParameterizedType]
          val typeArgs = parameterizedType.getActualTypeArguments
          val keyType = typeArgs(0)
          val valueType = typeArgs(1)
          val keyName = readName(keyType.asInstanceOf[Class[_]])
          val valueName = readName(valueType.asInstanceOf[Class[_]])
          docParam.paramType = "Map[" + keyName + "," + valueName + "]"
        } else
          if (!genericReturnType.getClass.isAssignableFrom(classOf[ParameterizedTypeImpl])){
            docParam.paramType = readName(genericReturnType.asInstanceOf[Class[_]])
          }
      }
      if (!"void".equals(docParam.paramType))
        documentationObject.addField(docParam)
    }
  }

  private def processAnnotations(name:String, annotations: Array[Annotation], docParam:DocumentationParameter):Boolean = {
    var isTransient = false
    for (ma <- annotations) {
      ma match {
        case xmlTransient: XmlTransient => {
          isTransient = true
        }
        case apiProperty: ApiProperty => {
          docParam.description = readString(apiProperty.value)
          docParam.notes = readString(apiProperty.notes)
          try {
            docParam.allowableValues = convertToAllowableValues(apiProperty.allowableValues)
          } catch {
            case e: RuntimeException => LOGGER.error("Allowable values annotation is wrong in for parameter " + docParam.name); e.printStackTrace();
          }
          docParam.paramAccess = readString(apiProperty.access)
        }
        case xmlAttribute: XmlAttribute => {
          docParam.name = readString(xmlAttribute.name, docParam.name, "##default")
          docParam.name = readString(name, docParam.name)
          docParam.required = xmlAttribute.required
        }
        case xmlElement: XmlElement => {
          docParam.name = readString(xmlElement.name, docParam.name, "##default")
          docParam.name = readString(name, docParam.name)
          docParam.defaultValue = readString(xmlElement.defaultValue, docParam.defaultValue, "\u0000")

          docParam.required = xmlElement.required
          val typeValueObj = xmlElementTypeMethod.invoke(xmlElement)
          val typeValue = if (typeValueObj == null) null else typeValueObj.asInstanceOf[Class[_]]
          //          docParam.paramType = readString(if (typeValue != null) typeValue.getName else null, docParam.paramType)
        }
        case xmlElementWrapper: XmlElementWrapper => {
          docParam.wrapperName = readString(xmlElementWrapper.name, docParam.wrapperName, "##default")
        }
        case _ => Unit
      }
    }
    isTransient
  }
}



trait BaseApiParser {
  val POSITIVE_INFINITY_STRING = "Infinity"
  val NEGATIVE_INFINITY_STRING = "-Infinity"

  private val logger = LoggerFactory.getLogger(classOf[BaseApiParser])

  protected def readString(s: String, existingValue: String = null, ignoreValue: String = null): String = {
    if (existingValue != null && existingValue.trim.length > 0) existingValue
    else if (s == null) null
    else if (s.trim.length == 0) null
    else if (ignoreValue != null && s.equals(ignoreValue)) null
    else s.trim
  }

  protected def toObjectList(csvString: String, paramType: String = null) = {
    if (csvString == null || csvString.length == 0) new ListBuffer[String].toList
    else {
      val params = csvString.split(",").toList
      paramType match {
        case null => params
        case "string" => params
      }
    }
  }

  protected def convertToAllowableValues(csvString: String, paramType: String = null): DocumentationAllowableValues = {
    if (csvString.toLowerCase.startsWith("range[")) {
      val ranges = csvString.substring(6, csvString.length() - 1).split(",")
      return buildAllowableRangeValues(ranges, csvString)
    } else if (csvString.toLowerCase.startsWith("rangeexclusive[")) {
      val ranges = csvString.substring(15, csvString.length() - 1).split(",")
      return buildAllowableRangeValues(ranges, csvString)
    } else {
      if (csvString == null || csvString.length == 0) {
        null
      } else {
        val params = csvString.split(",").toList
        paramType match {
          case null => new DocumentationAllowableListValues(params)
          case "string" => new DocumentationAllowableListValues(params)
        }
      }
    }
  }

  private def buildAllowableRangeValues(ranges: Array[String], inputStr: String): DocumentationAllowableRangeValues = {
    var min: java.lang.Float = 0
    var max: java.lang.Float = 0
    if (ranges.size < 2) {
      throw new RuntimeException("Allowable values format " + inputStr + "is incorrect")
    }
    if (ranges(0).equalsIgnoreCase(POSITIVE_INFINITY_STRING)) {
      min = Float.PositiveInfinity
    } else if (ranges(0).equalsIgnoreCase(NEGATIVE_INFINITY_STRING)) {
      min = Float.NegativeInfinity
    } else {
      min = ranges(0).toFloat
    }
    if (ranges(1).equalsIgnoreCase(POSITIVE_INFINITY_STRING)) {
      max = Float.PositiveInfinity
    } else if (ranges(1).equalsIgnoreCase(NEGATIVE_INFINITY_STRING)) {
      max = Float.NegativeInfinity
    } else {
      max = ranges(1).toFloat
    }
    val allowableValues = new DocumentationAllowableRangeValues(min, max)
    allowableValues
  }
}