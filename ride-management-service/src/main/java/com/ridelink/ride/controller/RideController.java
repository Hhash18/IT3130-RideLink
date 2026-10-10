package com.ridelink.ride.controller;


import com.ridelink.ride.dto.*;
import com.ridelink.ride.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;
@RestController
@RequestMapping("/api/rides")
@Tag(name="Ride Management")
@ApiResponses({
    @ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=ApiError.class))),
    @ApiResponse(responseCode="401",description="Authentication required",content=@Content(schema=@Schema(implementation=ApiError.class))),
    @ApiResponse(responseCode="403",description="Role or ownership denied",content=@Content(schema=@Schema(implementation=ApiError.class))),
    @ApiResponse(responseCode="404",description="Ride not found",content=@Content(schema=@Schema(implementation=ApiError.class))),
    @ApiResponse(responseCode="409",description="Invalid transition, unavailable driver or conflicting update",content=@Content(schema=@Schema(implementation=ApiError.class))),
    @ApiResponse(responseCode="502",description="Invalid dependency data or incompatible authentication",content=@Content(schema=@Schema(implementation=ApiError.class))),
    @ApiResponse(responseCode="503",description="Dependency unavailable",content=@Content(schema=@Schema(implementation=ApiError.class)))
})
@PreAuthorize("hasAnyRole('PASSENGER','DRIVER','ADMIN','SERVICE')")
public class RideController {
    private final RideService rides;
    private final FareIntegrationService billing;
    public RideController(RideService rides,FareIntegrationService billing) { this.rides=rides;this.billing=billing; }
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary="Create a REQUESTED ride with a fare estimate",description="Passenger identity comes from authentication. Each POST creates a new ride.")
    public ResponseEntity<RideResponse> create(@Valid @RequestBody RideRequest request) {
        var result=rides.create(request);return ResponseEntity.created(URI.create("/api/rides/"+result.id())).body(result);
    }
    @GetMapping("/{id}") @Operation(summary="Read a ride: owner, assigned driver, admin or service")
    public RideResponse get(@PathVariable UUID id) { return rides.get(id); }
    @GetMapping @Operation(summary="List authorized rides, paginated")
    public RidePage list(@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return rides.list(page,size); }
    @PutMapping("/{id}/assignment") @PreAuthorize("hasAnyRole('PASSENGER','ADMIN','SERVICE')")
    @Operation(summary="Assign the lowest-ID eligible driver in the requested service area")
    public RideResponse assign(@PathVariable UUID id) { return rides.assign(id); }
    @PutMapping("/{id}/acceptance") @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary="Assigned driver accepts an ASSIGNED ride")
    public RideResponse accept(@PathVariable UUID id) { return rides.accept(id); }
    @PutMapping("/{id}/start") @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary="Assigned driver starts an ACCEPTED ride")
    public RideResponse start(@PathVariable UUID id) { return rides.start(id); }
    @PutMapping("/{id}/completion") @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @Operation(summary="Complete an IN_PROGRESS ride and persist actual trip metrics",description="Completion commits before the separate final-fare call. Metrics cannot be overwritten by repeating completion.")
    public RideResponse complete(@PathVariable UUID id,@Valid @RequestBody CompleteRideRequest request) { return rides.complete(id,request); }
    @PutMapping("/{id}/cancellation") @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    @Operation(summary="Cancel a REQUESTED, ASSIGNED or ACCEPTED ride")
    public RideResponse cancel(@PathVariable UUID id,@Valid @RequestBody CancelRideRequest request) { return rides.cancel(id,request); }
    @PutMapping("/{id}/fare") @PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
    @Operation(summary="Request and record the final fare after completion",description="Safely repeatable after dependency failures. Does not record a payment; payment remains Member 4's responsibility.")
    public RideResponse finalizeFare(@PathVariable UUID id) { return billing.finalizeFare(id); }
}
