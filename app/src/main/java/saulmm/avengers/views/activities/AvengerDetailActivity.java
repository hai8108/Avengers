/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package saulmm.avengers.views.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.transition.Slide;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.bumptech.glide.Glide;
import javax.inject.Inject;
import saulmm.avengers.AnimUtils;
import saulmm.avengers.AvengersApplication;
import saulmm.avengers.R;
import saulmm.avengers.injector.components.DaggerAvengerInformationComponent;
import saulmm.avengers.injector.modules.ActivityModule;
import saulmm.avengers.injector.modules.AvengerInformationModule;
import saulmm.avengers.model.entities.Comic;
import saulmm.avengers.mvp.presenters.AvengerDetailPresenter;
import saulmm.avengers.mvp.views.AvengersDetailView;

public class AvengerDetailActivity extends AppCompatActivity implements AvengersDetailView {

    @InjectView(R.id.activity_avenger_detail_progress)      ProgressBar mProgress;
    @InjectView(R.id.activity_avenger_comics_progress)      ProgressBar mComicsProgress;
    @InjectView(R.id.activity_avenger_comics_container)     LinearLayout mDetailContainer;
    @InjectView(R.id.activity_avenger_detail_biography)     TextView mBiographyTextView;
    @InjectView(R.id.activity_avenger_detail_thumb)         ImageView mAvengerThumb;
    @InjectView(R.id.activity_avenger_thumb_background)     View mAvengerBackground;
    @InjectView(R.id.activity_avenger_name)                 TextView mAvengerNameTextView;

    @Inject AvengerDetailPresenter avengerDetailPresenter;

    private View comicView;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        initButterknife();
        initializeDependencyInjector();
        initializePresenter();
        initIncomingTransition();
    }

    private void initButterknife() {

        setContentView(R.layout.activity_avenger_detail);
        ButterKnife.inject(this);
    }

    @Override
    protected void onStart() {

        super.onStart();
        avengerDetailPresenter.onStart();
    }

    private void initializePresenter() {

        avengerDetailPresenter.attachView(this);
        avengerDetailPresenter.attachIncomingIntent(getIntent());
        avengerDetailPresenter.initializePresenter();
    }

    private void initializeDependencyInjector() {

        AvengersApplication avengersApplication = (AvengersApplication) getApplication();

        int avengerId = getIntent().getIntExtra(AvengersListActivity.EXTRA_CHARACTER_ID, -1);

        DaggerAvengerInformationComponent.builder()
            .activityModule(new ActivityModule(this))
            .appComponent(avengersApplication.getAppComponent())
            .avengerInformationModule(new AvengerInformationModule(avengerId))
            .build().inject(this);
    }

    private void initIncomingTransition() {

        final String sharedViewName = getIntent().getStringExtra(
            AvengersListActivity.EXTRA_IMAGE_TRANSITION_NAME);

        final Bitmap characterThumbBitmap = AvengersListActivity.sPhotoCache
            .get(AvengersListActivity.KEY_SHARED_BITMAP);

        Slide slideTransition = new Slide(Gravity.BOTTOM);
        slideTransition.excludeTarget(android.R.id.statusBarBackground, true);
        slideTransition.excludeTarget(R.id.activity_avenger_thumb_background, true);
        slideTransition.excludeTarget(android.R.id.navigationBarBackground, true);
        getWindow().setEnterTransition(slideTransition);

        mAvengerThumb.setImageBitmap(characterThumbBitmap);
        mAvengerThumb.setTransitionName(sharedViewName);

        mAvengerBackground.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                mAvengerBackground.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = mAvengerBackground.getWidth();
                int height = mAvengerBackground.getHeight();

                AnimUtils.showRevealEffect(mAvengerBackground, width/2, height/2, null);
                System.out.println("[DEBUG]" + " AvengerDetailActivity onGlobalLayout - " +
                    "");

            }
        });
    }

    @Override
    public void startLoading() {

        mProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void stopLoadingAvengersInformation() {

        mProgress.setVisibility(View.GONE);
    }

    @Override
    public void startLoadingComics() {

        mComicsProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void showAvengerBio(String text) {

        mBiographyTextView.setVisibility(View.VISIBLE);
        mBiographyTextView.setText(text);
    }

    @Override
    public void showAvengerImage(String url) {

        //Glide.with(this).load(url).into(mAvengerImageView);
    }

    @Override
    public void showAvengerName(String name) {

        mAvengerNameTextView.setText(name);
    }

    @Override
    public void addComic(Comic comic) {

        View comicView = LayoutInflater.from(this).inflate(
                R.layout.item_comic, null, true);

        TextView comicTitleTextView = ButterKnife.findById(comicView, R.id.item_comic_title);
        TextView comicDescTextView = ButterKnife.findById(comicView, R.id.item_comic_description);
        ImageView comicCoverImageView = ButterKnife.findById(comicView, R.id.item_comic_cover);

        comicTitleTextView.setText(comic.getTitle());

        if (comic.getFirstImageUrl() != null)
            Glide.with(this).load(comic.getFirstImageUrl()).into(comicCoverImageView);

        if (comic.getFirstTextObject() != null)
            comicDescTextView.setText(Html.fromHtml(comic.getFirstTextObject()));

        mDetailContainer.addView(comicView);
    }

    @Override
    public void stopLoadingComicsIfNeeded() {

        if (mComicsProgress.getVisibility() == View.VISIBLE)
            mComicsProgress.setVisibility(View.GONE);
    }

    @Override
    public void clearComicsView() {

        if(mDetailContainer.getChildCount() > 0)
            mDetailContainer.removeAllViews();
    }

    @Override
    public void showError(String errorMessage) {

        stopLoadingAvengersInformation();
        stopLoadingComicsIfNeeded();

        new AlertDialog.Builder(this)
            .setTitle("Error")
            .setPositiveButton("Accept", (dialog, which) -> finish())
            .setMessage(errorMessage)
            .setCancelable(false)
            .show();
    }

    @Override
    protected void onStop() {

        super.onStop();
        avengerDetailPresenter.onStop();
    }

    @OnClick(R.id.activity_avenger_detail_filter_button)
    public void onButtonClicked () {
        showFilterDialog();
    }

    public void showFilterDialog () {

        View filterView = LayoutInflater.from(this)
            .inflate(R.layout.view_filter_dialog, null);

        Spinner yearSpinner = ButterKnife.findById(filterView, R.id.view_filter_dialog_year_spinner);
        yearSpinner.setOnItemSelectedListener(avengerDetailPresenter);

        new AlertDialog.Builder(this)
            .setTitle("Filter")
            .setPositiveButton("Accept", (dialog, which) -> avengerDetailPresenter.onDialogButton(which))
            .setNegativeButton("Cancel", (dialog1, which) -> avengerDetailPresenter.onDialogButton(which))
            .setView(filterView)
            .show();
    }
}
