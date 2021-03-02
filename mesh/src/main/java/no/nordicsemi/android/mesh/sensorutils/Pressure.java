package no.nordicsemi.android.mesh.sensorutils;

import java.nio.ByteBuffer;
import java.util.Arrays;

import androidx.annotation.NonNull;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class Pressure extends DevicePropertyCharacteristic<Float> {
    public Pressure(@NonNull final byte[] data, final int offset) {
        super(data, offset);
        value = ByteBuffer.wrap(Arrays.copyOfRange(data, offset, offset + getLength())).order(LITTLE_ENDIAN).getInt() / 100.0f;
    }

    public Pressure(final Float pressure) {
        value = pressure;
    }

    @NonNull
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int getLength() {
        return 4;
    }

    @Override
    public byte[] getBytes() {
        return ByteBuffer.allocate(getLength()).order(LITTLE_ENDIAN).putInt(value.intValue()).array();
    }
}