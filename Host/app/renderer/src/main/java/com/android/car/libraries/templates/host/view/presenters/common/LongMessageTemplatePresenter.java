/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.libraries.templates.host.view.presenters.common;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.util.Objects.requireNonNull;

import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.TemplateWrapper;
import androidx.recyclerview.widget.RecyclerView;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.view.AbstractTemplatePresenter;
import com.android.car.libraries.apphost.view.common.ActionButtonListParams;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonListView;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonListView.Gravity;
import com.android.car.libraries.templates.host.view.widgets.common.HeaderView;
import com.android.car.libraries.templates.host.view.widgets.common.ParkedOnlyFrameLayout;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.recyclerview.CarUiRecyclerView.OnScrollListener;
import com.android.car.ui.widget.CarUiTextView;
import java.util.List;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * An {@link AbstractTemplatePresenter} that shows a scrolling long form message and some actions.
 */
public class LongMessageTemplatePresenter extends AbstractTemplatePresenter {
  // TODO(b/183643108): Use a common value for this constant
  private static final int MAX_ALLOWED_ACTIONS = 2;

  private final ViewGroup mRootView;
  private final HeaderView mHeaderView;
  private final CarUiRecyclerView mRecyclerView;
  private final ActionButtonListView mStickyActionButtonListView;
  private final ActionButtonListView.Gravity mActionButtonListGravity;
  private final ActionButtonListParams mActionButtonListParams;
  private final String mDisabledActionButtonToastMessage;

  private final LongMessageAdapter mAdapter;

  /** Create a LongMessageTemplatePresenter */
  static LongMessageTemplatePresenter create(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    LongMessageTemplatePresenter presenter =
        new LongMessageTemplatePresenter(templateContext, templateWrapper);
    presenter.update();
    return presenter;
  }

  @Override
  public void onTemplateChanged() {
    update();
  }

  @Override
  public View getView() {
    return mRootView;
  }

  /** Updates the view with current values in the {@link LongMessageTemplate}. */
  private void update() {
    LongMessageTemplate template = (LongMessageTemplate) getTemplate();
    ActionStrip actionStrip = template.getActionStrip();

    // If we have a title or a header action, show the header; hide it otherwise.
    CarText title = template.getTitle();
    Action headerAction = template.getHeaderAction();
    if (!CarText.isNullOrEmpty(title) || headerAction != null) {
      mHeaderView.setContent(getTemplateContext(), title, headerAction);
    } else {
      mHeaderView.setContent(getTemplateContext(), null, null);
    }

    mHeaderView.setActionStrip(actionStrip, ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE);

    mAdapter.setMessage(template.getMessage());
    if (mActionButtonListGravity == Gravity.CENTER) {
      // In the case of Gravity.CENTER, put the buttons in a row along with the rest of the content.
      mAdapter.setActions(template.getActions());
      mStickyActionButtonListView.setVisibility(GONE);
    } else {
      // If action button list gravity is not Gravity.CENTER, put the buttons in the sticky action
      // button list view so they stay on screen at all times.
      mAdapter.setActions(null);
      mStickyActionButtonListView.setVisibility(VISIBLE);
      mStickyActionButtonListView.setActionList(
          getTemplateContext(), template.getActions(), mActionButtonListParams);
    }

    // If this update is not due to a refresh, scroll back to the top. Template presenters can
    // be reused for templates of the same type, so a scroll reset would be needed for the case
    // where an app pushes two long message templates in the same flow, for example, or if we at
    // some point implement a pool of presenters.
    // TODO(b/186244619): Add unit test to cover this path.
    if (!getTemplateWrapper().isRefresh()) {
      mRecyclerView.scrollToPosition(0);
    }

    setActionButtonEnabledState();
  }

  private LongMessageTemplatePresenter(
      TemplateContext templateContext, TemplateWrapper templateWrapper) {
    super(templateContext, templateWrapper, StatusBarState.LIGHT);

    mRootView =
        (ViewGroup)
            LayoutInflater.from(templateContext)
                .inflate(R.layout.long_message_template_layout, null);

    mRecyclerView = mRootView.findViewById(R.id.list_view);

    ParkedOnlyFrameLayout contentContainer = mRootView.findViewById(R.id.park_only_container);
    mHeaderView = HeaderView.install(templateContext, contentContainer);
    contentContainer.setTemplateContext(templateContext);

    mStickyActionButtonListView = mRootView.requireViewById(R.id.sticky_action_button_list_view);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateActionButtonListGravity, R.attr.templatePlainContentBackgroundColor
    };

    TypedArray ta = templateContext.obtainStyledAttributes(themeAttrs);
    mActionButtonListGravity = ActionButtonListView.Gravity.values()[ta.getInt(0, 0)];
    @ColorInt int surroundingColor = ta.getColor(1, 0);
    ta.recycle();

    mDisabledActionButtonToastMessage =
        templateContext
            .getResources()
            .getString(
                templateContext.getHostResourceIds().getLongMessageTemplateDisabledActionText());

    mActionButtonListParams =
        ActionButtonListParams.builder()
            .setMaxActions(MAX_ALLOWED_ACTIONS)
            .setOemReorderingAllowed(true)
            .setOemColorOverrideAllowed(true)
            .setSurroundingColor(surroundingColor)
            .build();

    mAdapter = new LongMessageAdapter();
    mRecyclerView.setAdapter(mAdapter);

    mRecyclerView.addOnScrollListener(
        new OnScrollListener() {
          @Override
          public void onScrolled(CarUiRecyclerView recyclerView, int dx, int dy) {
            // no-op
          }

          @Override
          public void onScrollStateChanged(CarUiRecyclerView recyclerView, int newState) {
            if (newState != RecyclerView.SCROLL_STATE_IDLE) {
              return;
            }

            setActionButtonEnabledState();
          }
        });
    // {@link View#OnLayoutChangeListener} is required to disable sticky action buttons on first
    // load.
    mRecyclerView.addOnLayoutChangeListener(
        (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
            setActionButtonEnabledState());
  }

  @RequiresNonNull({
    "mRecyclerView",
    "mStickyActionButtonListView",
    "mDisabledActionButtonToastMessage"
  })
  private void setActionButtonEnabledState(
      @UnknownInitialization LongMessageTemplatePresenter this) {
    if (!mRecyclerView.getView().isAttachedToWindow()) {
      return;
    }
    // Only need to set active state for sticky action buttons since they stay on screen at all
    // times.
    if (mActionButtonListGravity == Gravity.CENTER) {
      return;
    }

    boolean enabled = !mRecyclerView.getView().canScrollVertically(/* direction= */ 1);

    if (enabled) {
      mStickyActionButtonListView.enableActionButtons();
    } else {
      mStickyActionButtonListView.disableActionButtons(mDisabledActionButtonToastMessage);
    }
  }

  /** Adapter used for rendering the long text and buttons in this template. */
  private class LongMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int ITEM_TYPE_MESSAGE = 1;
    static final int ITEM_TYPE_ACTION = 2;

    private String mMessage;
    @Nullable private List<Action> mActions;

    public void setMessage(CarText message) {
      mMessage = CarTextUtils.toCharSequenceOrEmpty(getTemplateContext(), message).toString();
      notifyDataSetChanged();
    }

    public void setActions(@Nullable List<Action> actions) {
      mActions = actions;
      notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
      if (position == 0) {
        return ITEM_TYPE_MESSAGE;
      } else {
        return ITEM_TYPE_ACTION;
      }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
      switch (viewType) {
        case ITEM_TYPE_ACTION:
          return new ActionsViewHolder(
              LayoutInflater.from(getTemplateContext())
                  .inflate(R.layout.long_message_action_layout, viewGroup, false));

        case ITEM_TYPE_MESSAGE:
        default:
          return new MessageViewHolder(
              LayoutInflater.from(getTemplateContext())
                  .inflate(R.layout.long_message_layout, viewGroup, false));
      }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
      if (viewHolder instanceof MessageViewHolder) {
        ((MessageViewHolder) viewHolder).bind(mMessage);
      } else if (viewHolder instanceof ActionsViewHolder) {
        ((ActionsViewHolder) viewHolder).bind(getTemplateContext(), requireNonNull(mActions));
      }
    }

    @Override
    public int getItemCount() {
      return mActions == null ? 1 : 2;
    }

    /** ViewHolder for a message list item */
    private class MessageViewHolder extends RecyclerView.ViewHolder {

      private final CarUiTextView mMessage;

      private MessageViewHolder(@NonNull View view) {
        super(view);
        mMessage = view.requireViewById(R.id.message_text);
      }

      private void bind(String message) {
        mMessage.setText(message);
      }
    }

    /** ViewHolder for actions list item */
    private class ActionsViewHolder extends RecyclerView.ViewHolder {

      private final ActionButtonListView mActionButtonListView;

      private ActionsViewHolder(@NonNull View view) {
        super(view);
        mActionButtonListView = view.requireViewById(R.id.action_button_list_view);
      }

      private void bind(TemplateContext templateContext, List<Action> actions) {
        if (!actions.isEmpty()) {
          mActionButtonListView.setActionList(templateContext, actions, mActionButtonListParams);
          mActionButtonListView.setVisibility(VISIBLE);
        } else {
          mActionButtonListView.setVisibility(GONE);
        }
      }
    }
  }
}
