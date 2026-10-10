package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.ApiError;
import com.ridelink.farepayment.dto.EstimateRequest;
import com.ridelink.farepayment.dto.EstimateResponse;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.service.FareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@ApiResponses({
        @ApiResponse(responseCode="400", description="Invalid input or missing required header", content=@Content(schema=@Schema(implementation=ApiError.class))),
        @ApiResponse(responseCode="401", description="Missing or invalid authentication", content=@Content(schema=@Schema(implementation=ApiError.class))),
        @ApiResponse(responseCode="403", description="Role or ownership does not permit this operation", content=@Content(schema=@Schema(implementation=ApiError.class)))
})
@Tag(name="Fares", description="Member 4 service. All payments are simulated.")
@PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN', 'SERVICE')")
public class FareController {
    private final FareService fares;
    public FareController(FareService fares) { this.fares = fares; }

    @PostMapping("/fares/estimate")
    @Operation(summary="Estimate a fare from simulated distance and time")
    public EstimateResponse estimate(@Valid @RequestBody EstimateRequest request) { return fares.estimate(request); }

    @ApiResponses({
            @ApiResponse(responseCode="404", description="Ride not found", content=@Content(schema=@Schema(implementation=ApiError.class))),
            @ApiResponse(responseCode="409", description="Ride not completed or concurrent finalization; retry same PUT", content=@Content(schema=@Schema(implementation=ApiError.class))),
            @ApiResponse(responseCode="502", description="Invalid Ride service data", content=@Content(schema=@Schema(implementation=ApiError.class))),
            @ApiResponse(responseCode="503", description="Ride service unavailable", content=@Content(schema=@Schema(implementation=ApiError.class)))
    })
    @PutMapping("/fares/rides/{rideId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    @Operation(summary="Finalize the fare for a completed ride", description="Fetches actual measurements from Ride Management. Repeated calls return the existing fare. Concurrent initial calls may return 409; retry the same PUT.")
    public FareResponse finalizeFare(@PathVariable @Pattern(regexp="[A-Za-z0-9_-]{1,100}") String rideId) { return fares.finalizeFare(rideId); }

    @GetMapping("/fares/rides/{rideId}")
    @Operation(summary="Retrieve a final fare (owner, admin or service)")
    public FareResponse fare(@PathVariable @Pattern(regexp="[A-Za-z0-9_-]{1,100}") String rideId) { return fares.get(rideId); }

}
