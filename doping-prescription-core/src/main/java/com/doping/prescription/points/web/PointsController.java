package com.doping.prescription.points.web;

import com.doping.prescription.auth.api.CurrentUser;
import com.doping.prescription.points.api.PointsLedger;
import com.doping.prescription.points.api.PointsLedger.Summary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** docs/api.md, GET /api/points: the total in the top bar. */
@RestController
class PointsController {

    private final PointsLedger points;

    PointsController(PointsLedger points) {
        this.points = points;
    }

    @GetMapping("/api/points")
    Summary points(CurrentUser user) {
        return points.summary(user.userId());
    }
}
