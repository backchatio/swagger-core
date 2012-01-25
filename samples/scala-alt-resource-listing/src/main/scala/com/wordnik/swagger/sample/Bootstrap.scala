package com.wordnik.swagger.sample

import com.wordnik.swagger.jaxrs._

import javax.servlet.http.HttpServlet

class Bootstrap extends HttpServlet {
  ApiReader.setFormatString("")
}