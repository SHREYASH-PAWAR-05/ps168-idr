# PS168 Data Schema — Day 1 draft (Member B)

## imu_log.csv
t_ns, sensor, x, y, z, w
- sensor ∈ {acc, gyro, mag, grav, rotvec}
- t_ns = SensorEvent.timestamp (Android elapsed-realtime nanoseconds, NOT wall clock)
- units: acc = m/s², gyro = rad/s, mag = µT, grav = m/s², rotvec = unitless quaternion (w present only for rotvec)
- rate: ~100 Hz per sensor (uncapped, Android decides actual rate)

## gnss_log.csv
t_ns, utc_ms, lat, lon, speed_mps, bearing_deg, accuracy_m
- t_ns = Location.elapsedRealtimeNanos (SAME clock as imu_log t_ns — use this to sync, not utc_ms)
- utc_ms = Location.getTime() (wall clock, for reference only)
- rate: ~1 Hz (GPS provider dependent)

## nav_state (for later days, not produced yet)
timestamp, px, py, vx, vy, heading, confidence, mode
mode ∈ {GNSS_GOOD, DEGRADED, DENIED, RECOVERY}

## Sample data
See sample_log_day1/ — recorded outdoors, 2 min walk, Redmi Note 13 Pro+, Android 16.