#pragma once

#include <cstdint>
#include <arm_neon.h>

uint8_t *warpPerspective(uint8_t *data_ptr, float32_t *rotation_ptr);

uint8_t *perspectiveTransform(uint8_t *data_ptr, float32_t *rotation_ptr);
