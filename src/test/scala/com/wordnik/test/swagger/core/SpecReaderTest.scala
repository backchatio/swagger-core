package com.wordnik.test.swagger.core

import org.codehaus.jackson.map._
import org.codehaus.jackson.map.DeserializationConfig.Feature
import org.codehaus.jackson.map.annotate.JsonSerialize
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector
import javax.xml.bind._
import javax.xml.bind.annotation._
import java.io.ByteArrayOutputStream
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import scala.reflect.BeanProperty
import java.io.ByteArrayInputStream
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import com.wordnik.swagger.core._
import javax.ws.rs._

@RunWith(classOf[JUnitRunner])
class SpecReaderTest extends FlatSpec with ShouldMatchers {
  it should "read a SimplePojo" in {
    var docObj = ApiPropertiesReader.read(classOf[SimplePojo])
    assert(null != docObj.getName)
  }

  it should "read a ScalaPojo" in {
    var docObj = ApiPropertiesReader.read(classOf[ScalaPojo])
    assert(null != docObj.getName)
  }

  it should "read a ScalaCaseClass" in {
    var docObj = ApiPropertiesReader.read(classOf[ScalaCaseClass])
    assert(null != docObj.getName)
  }

  it should "read a SimplePojo with XMLElement variations" in {
    var docObj = ApiPropertiesReader.read(classOf[SimplePojo2])
    assert((docObj.getFields.map(f=>f.name).toSet & Set("testInt","testString")).size === 2)
  }


  it should "read different data types properly " in {
    var docObj = ApiPropertiesReader.read(classOf[SampleDataTypes])
    var assertedFields = 0;
    for(field <- docObj.getFields.asScala){
      field.name match {
        case "sampleByte" => assert(field.paramType === "byte"); assertedFields += 1;
        case "sampleArrayByte" => assert(field.paramType === "Array[byte]"); assertedFields += 1;
        case "sampleListString" => assert(field.paramType === "Array[java.lang.String]"); assertedFields += 1;
        case _ =>
      }
    }
    assert(assertedFields === 3)
  }

  it should "read objects and its super class properties" in {
    var docObj = ApiPropertiesReader.read(classOf[ExtendedClass])
    var assertedFields = 0;
    for(field <- docObj.getFields.asScala){
      field.name match {
        case "stringProperty" => assert(field.paramType === "string");assertedFields += 1;
        case "intProperty" => assert(field.paramType === "int");assertedFields += 1;
        case "label" => assert(field.paramType === "string"); assertedFields += 1;
        case "transientFieldSerializedGetter" => assert(field.paramType === "string"); assertedFields += 1;
        case _ =>
      }
    }
    assert(assertedFields === 4)

  }

  it should "not create any model properties to default method like get class " in {
    var docObj = ApiPropertiesReader.read(classOf[ExtendedClass])
    var assertedFields = 0;
    for(field <- docObj.getFields.asScala){
      field.name match {
        case "class" => assert(false, "should not have class property in model object");
        case _ =>
      }
    }
  }

  it should "only read properties with XMLElement annotation if model object has XmlAccessType type NONE  annotation " in {
    var docObj = ApiPropertiesReader.read(classOf[TestObjectForNoneAnnotation])
    assert(null == docObj.getFields)

    docObj = ApiPropertiesReader.read(classOf[ScalaCaseClass])
    assert(docObj.getFields.size() === 1)

  }

  it should "read properties if attribute is defined as transient in the main class and xml element in the base class " in {
    var docObj = ApiPropertiesReader.read(classOf[TestTransientWithXMLElementInBaseCLass])
    assert(docObj.getFields.size() === 1)
  }

}

@RunWith(classOf[JUnitRunner])
class ResourceReaderTest extends FlatSpec with ShouldMatchers {
  behavior of "resource reader"

  it should "handle APIs that take array collection as post object" in {
    var doc = ApiReader.read(classOf[TestResourceJSON], "1.0", "1.0", "test", "apitest");
    var params = doc.getApis().get(0).getOperations().get(0).getParameters();
    var totalAssertions = 0;
    for (param <- params.asScala) {
      param.getName() match {
        case "users" =>
        case _ => {
          assert(param.getDataType() === "Array[java.lang.String]")
          assert(param.getValueTypeInternal() === "java.lang.String");
          totalAssertions += 1;
        }; //this mean it is post param hence no name
      }
    }
    assert(totalAssertions  === 1)
  }

  it should "handle apis that return collection objects " in {
    var doc = ApiReader.read(classOf[TestResourceJSON], "1.0", "1.0", "test", "apitest");
    var operation = doc.getApis().get(1).getOperations().get(0);
    assert(operation.getResponseTypeInternal() === "java.lang.String")
    assert(operation.getResponseClass() === "List[string]")
  }

  it should "handle APIs that take list collection as post object" in {
    var doc = ApiReader.read(classOf[TestResourceJSON], "1.0", "1.0", "test", "apitest");
    var params = doc.getApis().get(1).getOperations().get(0).getParameters();
    var totalAssertions = 0;
    for (param <- params.asScala) {
      param.getName() match {
        case "users" =>
        case _ => {
          assert(param.getDataType() === "List[string]")
          assert(param.getValueTypeInternal() === "string");
          totalAssertions += 1;
        }; //this mean it is post param hence no name
      }
    }
    assert(totalAssertions  === 1)
  }

}

@Path("/pet.json")
@Api(value = "/pet", description = "Operations about pets")
@Produces(Array("application/json"))
class TestResourceJSON {

  @POST
  @Path("/path1")
  @ApiOperation(value = "Sample operation that takes arry input")
  @ApiErrors(Array(new ApiError(code = 400, reason = "Invalid ID supplied"),
    new ApiError(code = 404, reason = "Pet not found"),
    new ApiError(code = 405, reason = "Validation exception") ))
  def testMethod1(@ApiParam(value = "List of users", required = true)users : Array[String]):String = {
    return "";
  }

  @POST
  @Path("/path2")
  @ApiOperation(value = "Sample operation that takes arry input", responseClass = "java.lang.String", multiValueResponse = true)
  @ApiErrors(Array(new ApiError(code = 400, reason = "Invalid ID supplied"),
    new ApiError(code = 404, reason = "Pet not found"),
    new ApiError(code = 405, reason = "Validation exception") ))
  def testMethod2(@ApiParam(value = "List of users", required = true)users : java.util.List[String]):String = {
    return "";
  }

}


@RunWith(classOf[JUnitRunner])
class JaxbSerializationTest extends FlatSpec with ShouldMatchers {
  it should "serialize a SimplePojo" in {
    val ctx = JAXBContext.newInstance(classOf[SimplePojo]);
    var m = ctx.createMarshaller()
    val e = new SimplePojo
    e.setTestInt(5)
    val baos = new ByteArrayOutputStream
    m.marshal(e, baos)
    assert(baos.toString == """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><simplePojo><testInt>5</testInt></simplePojo>""")
  }

  it should "deserialize a SimplePojo" in {
    val ctx = JAXBContext.newInstance(classOf[SimplePojo]);
    var u = ctx.createUnmarshaller()
    val b = new ByteArrayInputStream("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><simplePojo><testInt>5</testInt></simplePojo>""".getBytes)
    val p = u.unmarshal(b).asInstanceOf[SimplePojo]
    assert(p.getTestInt == 5)
  }

  it should "serialize a ScalaPojo" in {
    val ctx = JAXBContext.newInstance(classOf[ScalaPojo]);
    var m = ctx.createMarshaller()
    val e = new ScalaPojo
    e.testInt = 5
    val baos = new ByteArrayOutputStream
    m.marshal(e, baos)
  }

  it should "deserialize a ScalaPojo" in {
    val ctx = JAXBContext.newInstance(classOf[ScalaPojo]);
    var u = ctx.createUnmarshaller()
    val b = new ByteArrayInputStream("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><scalaishPojo><testInt>5</testInt></scalaishPojo>""".getBytes)
    val p = u.unmarshal(b).asInstanceOf[ScalaPojo]
    assert(p.testInt == 5)
  }

  it should "serialize a ScalaCaseClass" in {
    val ctx = JAXBContext.newInstance(classOf[ScalaCaseClass]);
    var m = ctx.createMarshaller()
    val e =  new ScalaCaseClass
    e.testInt = 5
    val baos = new ByteArrayOutputStream
    m.marshal(e, baos)
  }

  it should "deserialize a ScalaCaseClass" in {
    val ctx = JAXBContext.newInstance(classOf[ScalaCaseClass]);
    var u = ctx.createUnmarshaller()
    val b = new ByteArrayInputStream("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><scalaCaseClass><testInt>5</testInt></scalaCaseClass>""".getBytes)
    val p = u.unmarshal(b).asInstanceOf[ScalaCaseClass]
    assert(p.testInt == 5)
  }

  it should "serialize a ExtendedClass" in {
    val ctx = JAXBContext.newInstance(classOf[ExtendedClass]);
    var m = ctx.createMarshaller()
    val e = new ExtendedClass
    e.setTransientFieldSerializedGetter("Field1")
    e.setLabel("Field2")
    e.setStringProperty("Field3")
    m.marshal(e, System.out)
  }

  it should "serialize a TestObjectForNoneAnnotation with no xml element annotations" in {
    val ctx = JAXBContext.newInstance(classOf[TestObjectForNoneAnnotation]);
    var m = ctx.createMarshaller()
    val e = new TestObjectForNoneAnnotation
    e.setTestString("test String")
    m.marshal(e, System.out)
  }

}

@RunWith(classOf[JUnitRunner])
class JsonSerializationTest extends FlatSpec with ShouldMatchers {
  it should "serialize a SimplePojo" in {
    val mapper = getJsonMapper
    val e = new SimplePojo
    e.setTestInt(5)
    mapper.writeValueAsString(e)
  }

  it should "serialize a ScalaPojo" in {
    val mapper = getJsonMapper
    val e = new ScalaPojo
    e.testInt = 5
    mapper.writeValueAsString(e)
  }

  it should "serialize a ScalaCaseClass" in {
    val mapper = getJsonMapper
    val e =  new ScalaCaseClass
    e.testInt = 5
    mapper.writeValueAsString(e)
  }

  def getJsonMapper = {
    val jackson = new JacksonAnnotationIntrospector()
    val jaxb = new JaxbAnnotationIntrospector()
    val pair = new AnnotationIntrospector.Pair(jaxb, jackson)
    val mapper = new ObjectMapper()
    mapper.getSerializationConfig().setAnnotationIntrospector(jaxb)
    mapper.getDeserializationConfig().setAnnotationIntrospector(pair)
    mapper.getDeserializationConfig().set(Feature.AUTO_DETECT_SETTERS, true)
    mapper.configure(Feature.AUTO_DETECT_SETTERS, true)
    mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false)
    mapper
  }
}

@XmlRootElement(name= "BaseClass")
class BaseClass {
  @BeanProperty var stringProperty:String = _
  @BeanProperty var intProperty:Int = _

  @XmlTransient
  var label:String =_

  def setLabel(label: String) =
    this.label = label

  @XmlElement(name = "label")
  def getLabel() = label
}

@XmlRootElement(name= "ExtendedClass")
class ExtendedClass extends BaseClass{
  @BeanProperty var floatProperty:Float = _
  @BeanProperty var longProperty:Long = _

  @XmlTransient
  var transientFieldSerializedGetter:String =_

  def setTransientFieldSerializedGetter(value: String) =
    this.transientFieldSerializedGetter = value

  @XmlElement(name = "transientFieldSerializedGetter")
  def getTransientFieldSerializedGetter() = transientFieldSerializedGetter
}

@XmlRootElement(name= "sampleDataTypes")
class SampleDataTypes {
  @BeanProperty var sampleByte:Byte = _
  @BeanProperty var sampleArrayByte:Array[Byte] = _
  @BeanProperty var sampleArrayString:Array[String] = _
  @BeanProperty var sampleListString:Array[String] = _

}

@XmlRootElement(name = "simplePojo")
class SimplePojo {
  private var te: Int = 1
  private var testString: String = _

  @XmlElement(name = "testInt")
  def getTestInt: Int = te
  def setTestInt(te: Int) = { this.te = te }

  @XmlElement(name = "testString")
  def getTestString: String = testString
  def setTestString(testString: String) = { this.testString = testString}

}

@XmlRootElement(name = "simplePojo2")
@XmlAccessorType(XmlAccessType.NONE)
class SimplePojo2 {
  @XmlElement(name = "testInt")
  var te: Int = 1

  @XmlElement(name = "testString")
  var ts: String = _
}

@XmlRootElement(name = "scalaishPojo")
@XmlAccessorType(XmlAccessType.NONE)
class ScalaPojo {
  @XmlElement(name = "testInt")
  @BeanProperty
  var testInt = 0
}

@XmlRootElement(name = "scalaCaseClass")
@XmlAccessorType(XmlAccessType.NONE)
case class ScalaCaseClass() {
  @XmlElement(name = "testInt")
  @BeanProperty
  var testInt = 0

  @XmlTransient
  @BeanProperty
  var testTransient:List[String] = _
}


@XmlRootElement(name = "TestObjectForNoneAnnotation")
@XmlAccessorType(XmlAccessType.NONE)
case class TestObjectForNoneAnnotation() {
  @BeanProperty
  var testInt = 0

  @BeanProperty
  var testString:String = _
}


@XmlAccessorType(XmlAccessType.NONE)
trait Id {
  @XmlElement @BeanProperty var id: String = _
}

@XmlRootElement(name = "TestTransientWithXMLElementInBaseCLass")
@XmlAccessorType(XmlAccessType.NONE)
class TestTransientWithXMLElementInBaseCLass extends Id {
  @XmlTransient
  override def getId(): String = super.getId()
}


