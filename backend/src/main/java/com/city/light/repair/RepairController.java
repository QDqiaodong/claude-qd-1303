package com.city.light.repair;

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
@RequestMapping("/api/repairs")
public class RepairController {

    private final RepairService service;

    public RepairController(RepairService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Repair>> list(@RequestParam(required = false) Long lampId,
                                          @RequestParam(required = false) String zone) {
        return ApiResponse.success(service.list(lampId, zone));
    }

    @GetMapping("/{id}")
    public ApiResponse<Repair> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Repair> create(@RequestBody Repair in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Repair> update(@PathVariable Long id, @RequestBody Repair in) {
        return ApiResponse.success(service.update(id, in));
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<Repair> assign(@PathVariable Long id, @RequestParam String worker) {
        return ApiResponse.success(service.assign(id, worker));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<Repair> finish(@PathVariable Long id, @RequestBody Repair in) {
        return ApiResponse.success(service.finish(id, in));
    }
}
