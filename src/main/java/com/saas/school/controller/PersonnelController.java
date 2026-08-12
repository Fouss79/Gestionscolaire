package com.saas.school.controller;


import com.saas.school.dto.PersonnelRequest;
import com.saas.school.entity.Personnel;
import com.saas.school.service.PersonnelService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/personnels")
@RequiredArgsConstructor
public class PersonnelController {


    private final PersonnelService personnelService;



    @PostMapping
    public Personnel creer(
            @RequestBody PersonnelRequest request
    ){

        return personnelService.creerPersonnel(request);

    }




    @GetMapping("/ecole/{ecoleId}")
    public List<?> getByEcole(
            @PathVariable Long ecoleId
    ){

        return personnelService.getByEcole(ecoleId);

    }




    @GetMapping("/{id}")
    public Personnel getById(
            @PathVariable Long id
    ){

        return personnelService.getById(id);

    }




    @PutMapping("/{id}")
    public Personnel modifier(
            @PathVariable Long id,
            @RequestBody PersonnelRequest request
    ){

        return personnelService.modifier(id,request);

    }

}