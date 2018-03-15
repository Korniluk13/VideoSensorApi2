
#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc.hpp>

using namespace cv;

cv::Mat warpPerspective(cv::Mat & mat_rgb, cv::Mat & mat_rot) {
    Mat res;
    warpPerspective(mat_rgb, res, mat_rot, mat_rgb.size());
    return res;
}

cv::Mat perspectiveTransform(cv::Mat & mat_rgb, cv::Mat & mat_rot) {

    int type = mat_rgb.type();
    Size mSize = mat_rgb.size();

    int mHeight = mSize.height;
    int mWidth = mSize.width;

    Mat dst(mSize, type);

    static struct Init {
        Init(int mWidth, int mHeight) {
            mBuff = new float32_t[2 * mWidth * mHeight];
            for (int i = 0; i < mHeight; i++) {
                for (int j = 0; j < mWidth; j++) {
                    mBuff[2 * (i * mWidth + j)] = (float32_t)j;
                    mBuff[2 * (i * mWidth + j) + 1] = (float32_t)i;
                }
            }
        }

        float32_t * mBuff;
    } sBuff(mWidth, mHeight);

    Mat src1(1, mHeight * mWidth, CV_32FC2, sBuff.mBuff);
    Mat dst1(1, mHeight * mWidth, CV_32FC2);

    perspectiveTransform(src1, dst1, mat_rot);

    dst1 = dst1.reshape(2, mHeight);

    Mat empty;
    remap(mat_rgb, dst, dst1, empty, INTER_LINEAR);
    return dst;
}
