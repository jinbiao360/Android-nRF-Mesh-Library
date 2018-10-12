package no.nordicsemi.android.nrfmeshprovisioner;

import android.arch.lifecycle.ViewModelProvider;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import javax.inject.Inject;

import no.nordicsemi.android.meshprovisioner.meshmessagestates.MeshModel;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.messages.GenericOnOffGet;
import no.nordicsemi.android.meshprovisioner.messages.GenericOnOffSet;
import no.nordicsemi.android.meshprovisioner.messages.GenericOnOffStatus;
import no.nordicsemi.android.meshprovisioner.messages.MeshMessage;
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel;
import no.nordicsemi.android.meshprovisioner.utils.CompositionDataParser;
import no.nordicsemi.android.meshprovisioner.utils.Element;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

public class GenericOnOffServerActivity extends BaseModelConfigurationActivity {

    private static final String TAG = GenericOnOffServerActivity.class.getSimpleName();

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    private TextView onOffState;
    private TextView remainingTime;
    private Button mActionOnOff;
    protected int mTransitionStepResolution;
    protected int mTransitionSteps;

    @Override
    protected final void addControlsUi(final MeshModel model) {
        if (model instanceof GenericOnOffServerModel) {
            final CardView cardView = findViewById(R.id.node_controls_card);
            final View nodeControlsContainer = LayoutInflater.from(this).inflate(R.layout.layout_generic_on_off, cardView);
            final TextView time = nodeControlsContainer.findViewById(R.id.transition_time);
            onOffState = nodeControlsContainer.findViewById(R.id.on_off_state);
            remainingTime = nodeControlsContainer.findViewById(R.id.transition_state);
            final SeekBar transitionTimeSeekBar = nodeControlsContainer.findViewById(R.id.transition_seekbar);
            transitionTimeSeekBar.setProgress(0);
            transitionTimeSeekBar.incrementProgressBy(1);
            transitionTimeSeekBar.setMax(230);

            final SeekBar delaySeekBar = nodeControlsContainer.findViewById(R.id.delay_seekbar);
            delaySeekBar.setProgress(0);
            delaySeekBar.setMax(255);
            final TextView delayTime = nodeControlsContainer.findViewById(R.id.delay_time);

            mActionOnOff = nodeControlsContainer.findViewById(R.id.action_on_off);
            mActionOnOff.setOnClickListener(v -> {
                try {
                    if (mActionOnOff.getText().toString().equals(getString(R.string.action_generic_on))) {
                        sendGenericOnOff(true, delaySeekBar.getProgress());
                    } else {
                        sendGenericOnOff(false, delaySeekBar.getProgress());
                    }
                } catch (IllegalArgumentException ex) {
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            mActionRead = nodeControlsContainer.findViewById(R.id.action_read);
            mActionRead.setOnClickListener(v -> {
                sendGenericOnOffGet();
            });

            transitionTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int lastValue = 0;
                double res = 0.0;

                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {

                    if (progress >= 0 && progress <= 62) {
                        lastValue = progress;
                        mTransitionStepResolution = 0;
                        mTransitionSteps = progress;
                        res = progress / 10.0;
                        time.setText(getString(R.string.transition_time_interval, String.valueOf(res), "s"));
                    } else if (progress >= 63 && progress <= 118) {
                        if (progress > lastValue) {
                            mTransitionSteps = progress - 56;
                            lastValue = progress;
                        } else if (progress < lastValue) {
                            mTransitionSteps = -(56 - progress);
                        }
                        mTransitionStepResolution = 1;
                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps), "s"));

                    } else if (progress >= 119 && progress <= 174) {
                        if (progress > lastValue) {
                            mTransitionSteps = progress - 112;
                            lastValue = progress;
                        } else if (progress < lastValue) {
                            mTransitionSteps = -(112 - progress);
                        }
                        mTransitionStepResolution = 2;
                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "s"));
                    } else if (progress >= 175 && progress <= 230) {
                        if (progress >= lastValue) {
                            mTransitionSteps = progress - 168;
                            lastValue = progress;
                        } else if (progress < lastValue) {
                            mTransitionSteps = -(168 - progress);
                        }
                        mTransitionStepResolution = 3;
                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "min"));
                    }
                }

                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {

                }
            });

            delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                    delayTime.setText(getString(R.string.transition_time_interval, String.valueOf(progress * MeshParserUtils.GENERIC_ON_OFF_5_MS), "ms"));
                }

                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {

                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void enableClickableViews() {
        super.enableClickableViews();
        if (mActionOnOff != null && !mActionOnOff.isEnabled())
            mActionOnOff.setEnabled(true);
    }

    @Override
    protected void disableClickableViews() {
        super.disableClickableViews();
        if (mActionOnOff != null)
            mActionOnOff.setEnabled(false);
    }

    @Override
    protected void updateMeshMessage(final MeshMessage meshMessage) {
        super.updateMeshMessage(meshMessage);
        if (meshMessage instanceof GenericOnOffStatus) {
            hideProgressBar();
            final GenericOnOffStatus status = (GenericOnOffStatus) meshMessage;
            final boolean presentState = status.getPresentState();
            final Boolean targetOnOff = status.getTargetState();
            final int steps = status.getTransitionSteps();
            final int resolution = status.getTransitionResolution();
            if (targetOnOff == null) {
                if (presentState) {
                    onOffState.setText(R.string.generic_state_on);
                    mActionOnOff.setText(R.string.action_generic_off);
                } else {
                    onOffState.setText(R.string.generic_state_off);
                    mActionOnOff.setText(R.string.action_generic_on);
                }
                remainingTime.setVisibility(View.GONE);
            } else {
                if (!targetOnOff) {
                    onOffState.setText(R.string.generic_state_on);
                    mActionOnOff.setText(R.string.action_generic_off);
                } else {
                    onOffState.setText(R.string.generic_state_off);
                    mActionOnOff.setText(R.string.action_generic_on);
                }
                remainingTime.setText(getString(R.string.remaining_time, MeshParserUtils.getRemainingTransitionTime(resolution, steps)));
                remainingTime.setVisibility(View.VISIBLE);
            }
        }
    }


    /**
     * Send generic on off get to mesh node
     */
    public void sendGenericOnOffGet() {
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getMeshNode();
        final Element element = mViewModel.getSelectedElement().getElement();
        final MeshModel model = mViewModel.getSelectedModel().getMeshModel();

        if (!model.getBoundAppKeyIndexes().isEmpty()) {
            final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
            final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));

            final byte[] address = element.getElementAddress();
            Log.v(TAG, "Sending message to element's unicast address: " + MeshParserUtils.bytesToHex(address, true));

            final GenericOnOffGet genericOnOffSet = new GenericOnOffGet(node, appKey, 0);
            mViewModel.getMeshManagerApi().getGenericOnOff(address, genericOnOffSet);
            showProgressbar();
        } else {
            Toast.makeText(this, R.string.error_no_app_keys_bound, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Send generic on off set to mesh node
     *
     * @param state true to turn on and false to turn off
     * @param delay message execution delay in 5ms steps. After this delay milliseconds the model will execute the required behaviour.
     */
    public void sendGenericOnOff(final boolean state, final Integer delay) {
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getMeshNode();
        final Element element = mViewModel.getSelectedElement().getElement();
        final MeshModel model = mViewModel.getSelectedModel().getMeshModel();

        if (!model.getBoundAppKeyIndexes().isEmpty()) {
            final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
            final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));
            if (!model.getSubscriptionAddresses().isEmpty()) {
                final List<byte[]> addressList = model.getSubscriptionAddresses();
                for (int i = 0; i < addressList.size(); i++) {
                    final byte[] address = addressList.get(i);
                    Log.v(TAG, "Subscription addresses found for model: " + CompositionDataParser.formatModelIdentifier(model.getModelId(), true)
                            + ". Sending message to subscription address: " + MeshParserUtils.bytesToHex(address, true));
                    final GenericOnOffSet genericOnOffSet = new GenericOnOffSet(node, appKey, state, mTransitionSteps, mTransitionStepResolution, delay, 0);
                    mViewModel.getMeshManagerApi().setGenericOnOff(address, genericOnOffSet);
                }
            } else {
                final byte[] address = element.getElementAddress();
                Log.v(TAG, "No subscription addresses found for model: " + CompositionDataParser.formatModelIdentifier(model.getModelId(), true)
                        + ". Sending message to element's unicast address: " + MeshParserUtils.bytesToHex(address, true));
                final GenericOnOffSet genericOnOffSet = new GenericOnOffSet(node, appKey, state, mTransitionSteps, mTransitionStepResolution, delay, 0);
                mViewModel.getMeshManagerApi().setGenericOnOff(address, genericOnOffSet);
            }
            showProgressbar();
        } else {
            Toast.makeText(this, R.string.error_no_app_keys_bound, Toast.LENGTH_SHORT).show();
        }
    }
}