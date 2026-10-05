package com.city.light.lamp;

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
@RequestMapping("/api/lamps")
public class LampController {

    private final LampService service;

    public LampController(LampService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Lamp>> list(@RequestParam(required = false) Long roadId,
                                        @RequestParam(required = false) String zone) {
        return ApiResponse.success(service.list(roadId, zone));
    }

    @GetMapping("/{id}")
    public ApiResponse<Lamp> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Lamp> create(@RequestBody Lamp in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Lamp> update(@PathVariable Long id, @RequestBody Lamp in) {
        return ApiResponse.success(service.update(id, in));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Lamp> changeStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(service.changeStatus(id, status));
    }
}
