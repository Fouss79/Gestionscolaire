package com.saas.school.dto;


import lombok.Data;

@Data
public class ChangerRoleRequest {

    private Long utilisateurId;
    private Long roleId;

}