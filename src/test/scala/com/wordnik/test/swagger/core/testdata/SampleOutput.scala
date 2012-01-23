package com.wordnik.test.swagger.core.testdata

import com.wordnik.swagger.core._


import javax.xml.bind.annotation._

import scala.reflect.BeanProperty

@XmlRootElement (name="howdy")
@XmlAccessorType(XmlAccessType.NONE)
class SampleOutput {
  @XmlElement(name="id", required=true) 
  @ApiProperty(value = "unique identifier", allowableValues = "available,pending,sold")
  @BeanProperty var id: String = _
  @XmlElement(name="theName") @BeanProperty var name: String = _
  @XmlElement(name="theValue") @BeanProperty var value: String = _
}