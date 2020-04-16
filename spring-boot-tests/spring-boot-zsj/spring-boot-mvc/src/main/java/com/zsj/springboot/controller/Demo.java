package com.zsj.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping
public class Demo {

	@ResponseBody
	@RequestMapping("/hello")
	public String hello(){
		return "hello";
	}
}
