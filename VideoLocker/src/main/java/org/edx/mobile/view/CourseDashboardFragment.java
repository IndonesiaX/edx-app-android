package org.edx.mobile.view;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.inject.Inject;

import org.edx.mobile.R;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.api.CourseEntry;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.third_party.iconify.IconButton;
import org.edx.mobile.third_party.iconify.IconView;
import org.edx.mobile.third_party.iconify.Iconify;
import org.edx.mobile.util.ResourceUtil;
import org.edx.mobile.util.images.ShareUtils;
import org.edx.mobile.util.images.TopAnchorFillWidthTransformation;

import roboguice.fragment.RoboFragment;

public class CourseDashboardFragment extends RoboFragment {
    static public String TAG = CourseHandoutFragment.class.getCanonicalName();
    static public String CourseData = TAG + ".course_data";
    protected final Logger logger = new Logger(getClass().getName());
    @Inject
    IEdxEnvironment environment;
    private EnrolledCoursesResponse courseData;
    private boolean isCoursewareAccessible = true;
    private TextView courseTextName;
    private TextView courseTextDetails;
    private ImageView headerImageView;
    private LinearLayout parent;
    private TextView errorText;
    private ImageButton shareButton;

    @Inject
    private ISegment segIO;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        courseData = (EnrolledCoursesResponse) args.getSerializable(CourseData);
        if (courseData != null) {
            isCoursewareAccessible = courseData.getCourse().getCoursewareAccess().hasAccess();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (isCoursewareAccessible) {
            view = inflater.inflate(R.layout.fragment_course_dashboard, container, false);
            courseTextName = (TextView) view.findViewById(R.id.course_detail_name);
            courseTextDetails = (TextView) view.findViewById(R.id.course_detail_extras);
            headerImageView = (ImageView) view.findViewById(R.id.header_image_view);
            parent = (LinearLayout) view.findViewById(R.id.dashboard_detail);
            shareButton = (ImageButton) view.findViewById(R.id.course_detail_share); //invisible by default

        } else {
            view = inflater.inflate(R.layout.fragment_course_dashboard_disabled, container, false);
            errorText = (TextView) view.findViewById(R.id.error_msg);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (isCoursewareAccessible) {
            final LayoutInflater inflater = LayoutInflater.from(getActivity());

            if (courseData.isCertificateEarned() && environment.getConfig().areCertificateLinksEnabled()) {
                final View child = inflater.inflate(R.layout.row_course_dashboard_cert, parent, false);
                child.findViewById(R.id.get_certificate).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        environment.getRouter().showCertificate(getActivity(), courseData);
                    }
                });
                parent.addView(child);
            }

            //Implementation Note: - we can create a list view and populate the list.
            //but as number of rows are fixed and each row is different. the only common
            //thing is UI layout. so we reuse the same UI layout programmatically here.
            ViewHolder holder = createViewHolder(inflater, parent);

            holder.typeView.setIcon(Iconify.IconValue.fa_list_alt);
            holder.titleView.setText(R.string.courseware_title);
            holder.subtitleView.setText(R.string.courseware_subtitle);
            holder.rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    environment.getRouter().showCourseContainerOutline(getActivity(), courseData);
                }
            });


            if (courseData != null
                    && !TextUtils.isEmpty(courseData.getCourse().getDiscussionUrl())
                    && environment.getConfig().isDiscussionsEnabled()) {
                holder = createViewHolder(inflater, parent);

                holder.typeView.setIcon(Iconify.IconValue.fa_comments_o);
                holder.titleView.setText(R.string.discussion_title);
                holder.subtitleView.setText(R.string.discussion_subtitle);
                holder.rowView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        environment.getRouter().showCourseDiscussionTopics(getActivity(), courseData);
                    }
                });
            }

            holder = createViewHolder(inflater, parent);

            holder.typeView.setIcon(Iconify.IconValue.fa_file_text_o);
            holder.titleView.setText(R.string.handouts_title);
            holder.subtitleView.setText(R.string.handouts_subtitle);
            holder.rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (courseData != null)
                        environment.getRouter().showHandouts(getActivity(), courseData);
                }
            });

            holder = createViewHolder(inflater, parent);

            holder.typeView.setIcon(Iconify.IconValue.fa_bullhorn);
            holder.titleView.setText(R.string.announcement_title);
            holder.subtitleView.setText(R.string.announcement_subtitle);
            holder.rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (courseData != null)
                        environment.getRouter().showCourseAnnouncement(getActivity(), courseData);
                }
            });
        } else {
            errorText.setText(R.string.course_not_started);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (courseData == null || !isCoursewareAccessible) return;

        final String headerImageUrl = courseData.getCourse().getCourse_image(environment.getConfig());
        Glide.with(CourseDashboardFragment.this)
                .load(headerImageUrl)
                .placeholder(R.drawable.edx_map_login)
                .transform(new TopAnchorFillWidthTransformation(getActivity()))
                .into(headerImageView);

        courseTextName.setText(courseData.getCourse().getName());
        CourseEntry course = courseData.getCourse();
        courseTextDetails.setText(course.getDescription(getActivity(), true));

        if (environment.getConfig().isShareCourseEnabled()) {
            shareButton.setVisibility(headerImageView.VISIBLE);
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openShareMenu();
                }
            });
        }
    }

    /**
     * Creates a dropdown menu with appropriate apps when the share button is clicked.
     */
    private void openShareMenu() {
        final String shareText = ResourceUtil.getFormattedString(
                getResources(),
                R.string.share_course_message,
                "platform_name",
                getString(R.string.platform_name)).toString() + "\n" + courseData.getCourse().getCourse_about();
        ShareUtils.showShareMenu(
                ShareUtils.newShareIntent(shareText),
                getActivity().findViewById(R.id.course_detail_share),
                new ShareUtils.ShareMenuItemListener() {
                    @Override
                    public void onMenuItemClick(@NonNull ComponentName componentName) {
                        segIO.courseDetailShared(courseData.getCourse().getId(), shareText, componentName);
                        final Intent intent = ShareUtils.newShareIntent(shareText);
                        intent.setComponent(componentName);
                        startActivity(intent);
                    }
                },
                R.string.share_course_popup_header);
    }

    private ViewHolder createViewHolder(LayoutInflater inflater, LinearLayout parent) {
        ViewHolder holder = new ViewHolder();
        holder.rowView = inflater.inflate(R.layout.row_course_dashboard_list, parent, false);
        holder.typeView = (IconView) holder.rowView.findViewById(R.id.row_type);
        holder.titleView = (TextView) holder.rowView.findViewById(R.id.row_title);
        holder.subtitleView = (TextView) holder.rowView.findViewById(R.id.row_subtitle);
        parent.addView(holder.rowView);
        return holder;
    }

    private class ViewHolder {
        View rowView;
        IconView typeView;
        TextView titleView;
        TextView subtitleView;
    }
}
