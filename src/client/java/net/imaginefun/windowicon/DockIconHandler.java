package net.imaginefun.windowicon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.macosx.ObjCRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;

/**
 * Replaces the OS window icon (macOS Dock, Windows taskbar) with the ImagineFun logo
 * while connected to an ImagineFun server, and restores the vanilla icon on disconnect.
 *
 * <p>Two code paths because GLFW's {@code glfwSetWindowIcon} is documented as a no-op on
 * macOS — Cocoa requires {@code [NSApplication setApplicationIconImage:]} via the
 * Objective-C runtime.
 *
 * <p>Must be called <em>after</em> the GLFW window exists; calling during
 * {@code onInitializeClient} is too early because window creation overwrites the icon.
 */
public final class DockIconHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("ImagineFunDockIcon");
    private static final String ICON_RESOURCE = "/assets/imaginefunutils/dock-icon.png";

    private enum Os { MAC, WIN, OTHER }

    private static final Os OS;
    static {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            OS = Os.MAC;
        } else if (os.contains("win")) {
            OS = Os.WIN;
        } else {
            OS = Os.OTHER;
        }
    }

    private static boolean applied = false;
    private static long savedMacIcon = 0;

    private DockIconHandler() {}

    public static void apply() {
        if (applied) return;
        switch (OS) {
            case MAC -> applyMac();
            case WIN -> applyGlfw();
            case OTHER -> {}
        }
    }

    public static void reset() {
        if (!applied) return;
        switch (OS) {
            case MAC -> resetMac();
            case WIN -> resetGlfw();
            case OTHER -> {}
        }
    }

    private static byte[] readIconBytes() throws IOException {
        try (InputStream in = DockIconHandler.class.getResourceAsStream(ICON_RESOURCE)) {
            if (in == null) {
                throw new IOException("missing classpath resource: " + ICON_RESOURCE);
            }
            return in.readAllBytes();
        }
    }

    private static void applyMac() {
        try {
            byte[] pngBytes = readIconBytes();

            long msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
            if (msgSend == 0) {
                LOGGER.warn("objc_msgSend not found; cannot set Dock icon");
                return;
            }

            long nsAppClass = ObjCRuntime.objc_getClass("NSApplication");
            long selSharedApp = ObjCRuntime.sel_registerName("sharedApplication");
            long app = JNI.invokePPP(nsAppClass, selSharedApp, msgSend);

            long selGetIcon = ObjCRuntime.sel_registerName("applicationIconImage");
            savedMacIcon = JNI.invokePPP(app, selGetIcon, msgSend);

            long nsDataClass = ObjCRuntime.objc_getClass("NSData");
            long selDataWithBytesLength = ObjCRuntime.sel_registerName("dataWithBytes:length:");

            long pngPtr = MemoryUtil.nmemAlloc(pngBytes.length);
            try {
                for (int i = 0; i < pngBytes.length; i++) {
                    MemoryUtil.memPutByte(pngPtr + i, pngBytes[i]);
                }

                long nsData = JNI.invokePPPP(
                    nsDataClass, selDataWithBytesLength, pngPtr, pngBytes.length, msgSend);
                if (nsData == 0) {
                    LOGGER.warn("Failed to create NSData from PNG bytes");
                    return;
                }

                long nsImageClass = ObjCRuntime.objc_getClass("NSImage");
                long selAlloc = ObjCRuntime.sel_registerName("alloc");
                long selInitWithData = ObjCRuntime.sel_registerName("initWithData:");

                long nsImageRaw = JNI.invokePPP(nsImageClass, selAlloc, msgSend);
                long nsImage = JNI.invokePPPP(nsImageRaw, selInitWithData, nsData, msgSend);
                if (nsImage == 0) {
                    LOGGER.warn("Failed to create NSImage from PNG data");
                    return;
                }

                long selSetIcon = ObjCRuntime.sel_registerName("setApplicationIconImage:");
                JNI.invokePPPV(app, selSetIcon, nsImage, msgSend);

                applied = true;
                LOGGER.info("Replaced macOS Dock icon with ImagineFun logo");
            } finally {
                MemoryUtil.nmemFree(pngPtr);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to set macOS Dock icon", e);
        }
    }

    private static void resetMac() {
        try {
            long msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
            if (msgSend == 0) return;

            long nsAppClass = ObjCRuntime.objc_getClass("NSApplication");
            long selSharedApp = ObjCRuntime.sel_registerName("sharedApplication");
            long app = JNI.invokePPP(nsAppClass, selSharedApp, msgSend);

            long selSetIcon = ObjCRuntime.sel_registerName("setApplicationIconImage:");
            JNI.invokePPPV(app, selSetIcon, savedMacIcon, msgSend);

            applied = false;
            LOGGER.info("Restored original macOS Dock icon");
        } catch (Exception e) {
            LOGGER.warn("Failed to restore macOS Dock icon", e);
        }
    }

    private static void applyGlfw() {
        NativeImage image = null;
        ByteBuffer pixels = null;
        try (InputStream in = DockIconHandler.class.getResourceAsStream(ICON_RESOURCE)) {
            if (in == null) {
                LOGGER.warn("Dock icon resource missing: {}", ICON_RESOURCE);
                return;
            }
            image = NativeImage.read(in);
            int w = image.getWidth();
            int h = image.getHeight();

            // GLFW expects RGBA bytes in memory order; NativeImage.getPixel returns
            // 0xAARRGGBB packed ints, so unpack and rewrite as R,G,B,A bytes.
            pixels = MemoryUtil.memAlloc(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = image.getPixel(x, y);
                    pixels.put((byte) ((argb >> 16) & 0xFF));
                    pixels.put((byte) ((argb >> 8) & 0xFF));
                    pixels.put((byte) (argb & 0xFF));
                    pixels.put((byte) ((argb >> 24) & 0xFF));
                }
            }
            pixels.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                GLFWImage glfwImg = GLFWImage.malloc(stack);
                glfwImg.set(w, h, pixels);
                GLFWImage.Buffer buf = GLFWImage.malloc(1, stack);
                buf.put(0, glfwImg);
                buf.position(0);
                long handle = Minecraft.getInstance().getWindow().handle();
                GLFW.glfwSetWindowIcon(handle, buf);
            }

            applied = true;
            LOGGER.info("Replaced window icon with ImagineFun logo");
        } catch (Exception e) {
            LOGGER.warn("Failed to set window icon", e);
        } finally {
            if (pixels != null) MemoryUtil.memFree(pixels);
            if (image != null) image.close();
        }
    }

    private static void resetGlfw() {
        try {
            Minecraft mc = Minecraft.getInstance();
            IconSet set = SharedConstants.getCurrentVersion().stable()
                ? IconSet.RELEASE
                : IconSet.SNAPSHOT;
            mc.getWindow().setIcon(mc.getVanillaPackResources(), set);
            applied = false;
            LOGGER.info("Restored vanilla window icon");
        } catch (Exception e) {
            LOGGER.warn("Failed to restore window icon", e);
        }
    }
}
