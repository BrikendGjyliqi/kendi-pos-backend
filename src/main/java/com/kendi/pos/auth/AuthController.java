package com.kendi.pos.auth;

import com.kendi.pos.staff.Staff;
import com.kendi.pos.staff.StaffRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final StaffRepository staffRepository;

    public AuthController(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getPin() == null || request.getPin().isEmpty()) {
            return ResponseEntity.badRequest().body("PIN i nevojshëm");
        }

        // Gjej staffin qe ka kete PIN
        List<Staff> allStaff = staffRepository.findAll();
        Staff matched = null;
        for (Staff s : allStaff) {
            if (s.isActive() && BCrypt.checkpw(request.getPin(), s.getPinHash())) {
                matched = s;
                break;
            }
        }

        if (matched == null) {
            return ResponseEntity.status(401).body("PIN i pasaktë");
        }

        // Token i thjeshte per fillim (do bejme JWT me pas)
        String token = UUID.randomUUID().toString();

        return ResponseEntity.ok(new LoginResponse(
                token,
                matched.getId(),
                matched.getName(),
                matched.getRole()
        ));
    }
}