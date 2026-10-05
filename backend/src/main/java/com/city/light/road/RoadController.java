package com.city.light.road;

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
@RequestMapping("/api/roads")
public class RoadController {

    private final RoadService service;

    public RoadController(RoadService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Road>> list(@RequestParam(required = false) String zone) {
        return ApiResponse.success(service.list(zone));
    }

    @GetMapping("/{id}")
    public ApiResponse<Road> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Road> create(@RequestBody Road in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Road> update(@PathVariable Long id, @RequestBody Road in) {
        return ApiResponse.success(service.update(id, in));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Road> changeStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(service.changeStatus(id, status));
    }

    @PostMapping("/{id}/sync-lamp-count")
    public ApiResponse<Road> syncLampCount(@PathVariable Long id) {
        return ApiResponse.success(service.syncLampCount(id));
    }
}
