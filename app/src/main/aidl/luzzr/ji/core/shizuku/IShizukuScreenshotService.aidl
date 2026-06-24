package luzzr.ji.core.shizuku;

import android.os.ParcelFileDescriptor;

interface IShizukuScreenshotService {
    ParcelFileDescriptor capturePng();
}
