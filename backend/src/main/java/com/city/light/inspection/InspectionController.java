package com.city.light.inspection;

import com.city.light.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inspections")
public class InspectionController {

    private final InspectionService service;

    public InspectionController(InspectionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Inspection>> list(@RequestParam(required = false) Long roadId,
                                              @RequestParam(required = false) String zone) {
        return ApiResponse.success(service.list(roadId, zone));
    }

    @GetMapping("/{id}")
    public ApiResponse<Inspection> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Inspection> create(@RequestBody Inspection in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Inspection> update(@PathVariable Long id, @RequestBody Inspection in) {
        return ApiResponse.success(service.update(id, in));
    }
}
