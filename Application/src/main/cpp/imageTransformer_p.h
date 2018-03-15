#pragma once

#include <opencv2/core/mat.hpp>

cv::Mat warpPerspective(cv::Mat & mat_rgb, cv::Mat & mat_rot);

cv::Mat perspectiveTransform(cv::Mat & mat_rgb, cv::Mat & mat_rot);
