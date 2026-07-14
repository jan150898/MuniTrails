package com.example.trails.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UploadPageController {

    @GetMapping("/upload")
    public String upload() {
        return "upload";
    }
}
