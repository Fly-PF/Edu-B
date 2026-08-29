package com.edu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
@ActiveProfiles("prod")
class EduBApplicationTests {

    @Test
    void contextLoads() {
        // superadmin 密码：12345678
        System.out.println(new BCryptPasswordEncoder().encode("12345678"));
        // admin001 密码：Admin@123456
        System.out.println(new BCryptPasswordEncoder().encode("Admin@123456"));
        // safety_s001 密码：Student@123456
        System.out.println(new BCryptPasswordEncoder().encode("Student@123456"));
        // teacher001 密码：Teacher@123456
        System.out.println(new BCryptPasswordEncoder().encode("Teacher@123456"));
    }
}
