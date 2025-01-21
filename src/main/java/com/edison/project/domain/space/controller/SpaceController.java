package com.edison.project.domain.space.controller;

import com.edison.project.domain.space.entity.Space;
import com.edison.project.domain.space.service.SpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces")
public class SpaceController {

    @Autowired
    private SpaceService spaceService;

    // [POST] Space OPENAI API 이용하여 처리
    @PostMapping("/process")
    public ResponseEntity<List<Space>> processSpaces() {
        List<Space> processedSpaces = spaceService.processSpaces();
        return ResponseEntity.ok(processedSpaces);
    }
}
