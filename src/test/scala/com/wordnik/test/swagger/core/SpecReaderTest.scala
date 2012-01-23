package com.wordnik.test.swagger.core

import com.wordnik.swagger.core.ApiPropertiesReader

import javax.xml.bind._
import javax.xml.bind.annotation._
import java.io.{ ByteArrayOutputStream, ByteArrayInputStream }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

@RunWith(classOf[JUnitRunner])
class SpecReaderTest extends FlatSpec with ShouldMatchers {
  it should "read a SimplePojo" in {
    var docObj = ApiPropertiesReader.read(classOf[SimplePojo])
    assert((docObj.getFields.map(f=>f.name).toSet & Set("testInt","testString")).size === 2)
  }

  it should "read a ScalaPojo" in {
    var docObj = ApiPropertiesReader.read(classOf[ScalaPojo])
    assert((docObj.getFields.map(f=>f.name).toSet & Set("testInt")).size === 1)
  }

  it should "read a ScalaCaseClass" in {
    var docObj = ApiPropertiesReader.read(classOf[ScalaCaseClass])
    assert((docObj.getFields.map(f=>f.name).toSet & Set("testInt")).size === 1)
  }

  it should "read a SimplePojo with XMLElement variations" in {
    var docObj = ApiPropertiesReader.read(classOf[SimplePojo2])
    println(docObj.getFields)
    println(docObj.getFields.map(f=>f.name).toSet)
    assert((docObj.getFields.map(f=>f.name).toSet & Set("testInt","testString")).size === 2)
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
    val e = new ScalaCaseClass
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
}

@RunWith(classOf[JUnitRunner])
class JsonSerializationTest extends FlatSpec with ShouldMatchers {
  it should "serialize a SimplePojo" in {
    val mapper = JsonUtil.getJsonMapper
    val e = new SimplePojo
    e.setTestInt(5)
    mapper.writeValueAsString(e)
  }

  it should "serialize a ScalaPojo" in {
    val mapper = JsonUtil.getJsonMapper
    val e = new ScalaPojo
    e.testInt = 5
    mapper.writeValueAsString(e)
  }

  it should "serialize a ScalaCaseClass" in {
    val mapper = JsonUtil.getJsonMapper
    val e = new ScalaCaseClass
    e.testInt = 5
    mapper.writeValueAsString(e)
  }
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
  def setTestString(testString: String) = { this.testString = testString }
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
  @XmlElement(name = "testInt") @BeanProperty
  var testInt = 0
}

@XmlRootElement(name = "scalaCaseClass")
@XmlAccessorType(XmlAccessType.NONE)
case class ScalaCaseClass() {
  @XmlElement(name = "testInt") @BeanProperty
  var testInt = 0

  @XmlTransient @BeanProperty
  var testTransient: List[String] = _
}