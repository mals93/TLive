package com.testexam.charlie.tlive.main.place.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.testexam.charlie.tlive.R
import com.testexam.charlie.tlive.common.BaseActivity
import com.testexam.charlie.tlive.common.HttpTask
import com.testexam.charlie.tlive.common.Params
import com.testexam.charlie.tlive.main.place.Place
import com.testexam.charlie.tlive.main.place.detail.pathFinder.NavigationActivity
import com.testexam.charlie.tlive.main.place.detail.pathFinder.SelectPathFinderBottomSheet
import com.testexam.charlie.tlive.main.place.detail.photo.Photo
import com.testexam.charlie.tlive.main.place.detail.photo.PhotoAdapter
import com.testexam.charlie.tlive.main.place.detail.review.Review
import com.testexam.charlie.tlive.main.place.detail.review.ReviewActivity
import com.testexam.charlie.tlive.main.place.detail.review.ReviewAdapter
import com.testexam.charlie.tlive.main.place.detail.webview.SearchNaverActivity
import com.testexam.charlie.tlive.main.place.write.WriteReviewActivity
import kotlinx.android.synthetic.main.activity_place_detail.*
import kotlinx.android.synthetic.main.content_detail_basic_info.*
import kotlinx.android.synthetic.main.content_detail_info.*
import kotlinx.android.synthetic.main.content_detail_location.*
import kotlinx.android.synthetic.main.content_detail_review.*
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.text.DecimalFormat


/**
 * 맛집에 대한 상세 정보를 보여주는 Activity
 */
private const val WRITE_CODE = 1000 // startActivityForResult 에서 사용하는 쓰기 코드
class PlaceDetailActivity : BaseActivity(), View.OnClickListener, OnMapReadyCallback {
    private lateinit var photoAdapter: PhotoAdapter     // 미리보기 사진의 어댑터
    private lateinit var photoList : ArrayList<Photo>   // 미리보기 사진 리스트

    private lateinit var reviewAdapter: ReviewAdapter   // 리뷰 어댑터
    private lateinit var reviewList : ArrayList<Review> // 리뷰 리스트

    private lateinit var place : Place  // 맛집 정보를 가지고 있는 Place 객체

    private var userEmail = "" // 현재 로그인한 사용자의 이메일

    private var address = ""        // 맛집의 주소
    private var favoriteNum = 0     // 맛집의 가고싶다 개수
    private var workingTime = ""    // 맛집의 근무 시간
    private var breakTime = ""      // 맛집의 휴게 시간
    private var price = ""          // 맛집의 대략적인 가격대
    private var reviewCount = 0     // 총 리뷰의 개수
    private var goodReviewCount = 0     // 좋아요 리뷰의 개수
    private var normalReviewCount = 0   // 괜찮다 리뷰의 개수
    private var badReviewCount = 0      // 별로 리뷰의 개수
    private var isFavorite = false      // 내가 현재 보고 있는 맛집을 가고싶다 했는지 여부를 저장하는 변수

    private lateinit var myLatLng: LatLng       // 나의 위경도 정보
    private lateinit var menuArray : JSONArray  // 메뉴 리스트

    private lateinit var photoJSONArray : JSONArray //

    private lateinit var reviewJSONArray : JSONArray

    private val decimalFormat = DecimalFormat("#,###")

    private lateinit var scaleAnim : Animation      // 가고싶다 버튼을 누를때 커졌다 작아지는 애니메이션

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_detail)

        // 지도 객체 선언
        val mapFragment = supportFragmentManager.findFragmentById(R.id.detailMap) as SupportMapFragment
        Thread{mapFragment.getMapAsync(this)}.run()

        place = intent.getParcelableExtra("place") // 선택한 맛집의 정보가 담겨있는 Place 객체를 Intent 에서 가져온다.

        userEmail = getSharedPreferences("login", Context.MODE_PRIVATE).getString("email","none") // 이메일을 가져온다.

        val myLat = intent.getDoubleExtra("lat",0.0) // 현재 나의 위도
        val myLon = intent.getDoubleExtra("lon",0.0) // 현재 나의 경도
        myLatLng = LatLng(myLat,myLon)  // 위경도를 이용하여 나의 위치를 나타내는 LatLon 객체를 초기화한다.

        getDetailInfo(place.placeNo)    // 맛집의 상세 정보를 불러온다.

        setOnClickListeners() // 클릭 리스너 등록
        setRecyclerViews() // RecyclerView 설정

        scaleAnim = AnimationUtils.loadAnimation(applicationContext,R.anim.scale_change_btn)    // 애니메이션 초기화
    }

    /*
     * 맛집의 상세 정보를 서버에서 불러오는 메소드
     *
     * 서버에서 정보를 불러온 다음 View 에 보여주는 setInformation() 메소드를 호출한다.
     */
    private fun getDetailInfo(placeNo : Int){
        runOnUiThread({
            detailPb.visibility = View.VISIBLE      // 프로그레스 바를 표시한다.
        })
        Thread{
            try{
                val params = ArrayList<Params>()    // 파라미터를 담고 있는 ArrayList
                params.add(Params("placeNo",placeNo.toString()))    // placeNo 를 파라미터에 추가한다.
                params.add(Params("userEmail",userEmail))       // userEmail 를 파라미터에 추가한다.
                val result = HttpTask("getDetailPlace.php",params).execute().get()  // 현재 맛집에 대한 상세 정보를 서버에 요청한다. 결과 값은 result 에 저장한다.

                if(result != null){ // 결과 값이 존재하는 경우
                    val jsonObject = JSONObject(result) // JSONObject 로 result를 변환한다.
                    address = jsonObject.getString("address")               // 맛집의 주소
                    favoriteNum = jsonObject.getInt("favoriteNum")          // 좋아요 개수
                    workingTime = jsonObject.getString("workingTime")       // 근무 시간
                    breakTime = jsonObject.getString("breakTime")           // 휴게 시간
                    price = jsonObject.getString("price")                   // 가격대
                    reviewCount = jsonObject.getInt("reviewCount")          // 리뷰의 총 개수
                    goodReviewCount = jsonObject.getInt("goodCount")        // 좋아요 리뷰의 개수
                    normalReviewCount = jsonObject.getInt("normalCount")    // 괜찮다 리뷰의 개수
                    badReviewCount = jsonObject.getInt("badCount")          // 별로 리뷰의 개수
                    isFavorite = jsonObject.getBoolean("isWant")            // 내가 현재 보고 있는 맛집을 가고싶다 했는지 여부를 저장하는 변수

                    menuArray = JSONArray(jsonObject.getString("menu"))     // 메뉴
                    val photoArray = jsonObject.getString("photoArray") // 사진 미리보기 배열의 경로가 담겨있는 photoArray
                    Timber.d("detail photoArray $photoArray")
                    // 표시할 사진이 있는 경우
                    if(photoArray != "null"){
                        photoList.clear()       // 미리보기 사진 리스트를 초기화한다.
                        photoJSONArray = JSONArray(photoArray)
                        for(i in 0 until photoJSONArray.length()){
                            val photoObject = photoJSONArray.getJSONObject(i)
                            val src = photoObject.getString("src")
                            photoList.add(Photo(src))   // 사진 리스트에 Photo 객체를 추가한다.
                        }
                    }
                    // 리뷰가 있는 경우
                    val reviews = jsonObject.getString("reviews")
                    if(reviews != "null"){ // reviews 가 null 이거나 blank 가 아니라면
                        reviewJSONArray = JSONArray(reviews)
                        reviewList.clear()
                        for( i in 0 until reviewJSONArray.length()){
                            val reviewObject = reviewJSONArray.getJSONObject(i)
                            reviewList.add(Review(
                                    reviewObject.getString("userEmail"),        // 리뷰 작성자 이메일
                                    reviewObject.getString("userName"),         // 리뷰 작성자 이름
                                    reviewObject.getString("userProfile"),      // 리뷰 작성자 프로필 사진 경로
                                    reviewObject.getInt("userReviewCount"),     // 리뷰 작성자가 작성한 리뷰의 개수
                                    reviewObject.getString("reviewText"),       // 리뷰 텍스트 본문
                                    reviewObject.getInt("reviewPoint"),         // 리뷰 점수
                                    reviewObject.getString("photoArray"),       // 리뷰할 때 같이 보낸 사진의 jsonArray (String 으로 저장된다.)
                                    reviewObject.getString("uploadTime")        // 리뷰 작성한 날짜와 시간
                            ))
                        }
                    }
                    setInformation() // 정보를 View 에 설정한다.
                }
            }catch (e : Exception ){
                e.printStackTrace()
            }finally {

            }
        }.start()

    }

    /*
     * 맛집 정보들을 View 에 설정하는 메소드
     */
    @SuppressLint("SetTextI18n")
    private fun setInformation(){
        runOnUiThread({
            detailTitleTv.text = place.name // 타이틀 바에 맛집 이름 설정
            detailNameTv.text = place.name  // detailNameTv 에 맛집 이름 설정
            detailViewerTv.text = place.viewNum.toString() // 정보 열람 회수 설정
            detailStarTv.text = favoriteNum.toString() // 가고싶다 개수 설정
            detailReviewTv.text = place.reviewNum.toString() // 리뷰 수 설정
            detailStarScoreTv.text = place.starScore.toString() // 맛집 평점 설정

            detailAddressTv.text = address  // 주소 설정
            detailWorkingTimeTv.text = workingTime // 영업 시간
            if(breakTime != ""){    // 쉬는 시간이 있으면 쉬는 시간을 표시하고 없다면 View 를 안보이게 한다.
                detailBreakTimeTv.text = breakTime
            }else{
                detailBreakTimeTv.visibility = View.GONE
                detailBreakTv.visibility = View.GONE
            }
            detailPriceTv.text = price // 가격대 정보를 설정한다.
            detailMenu1Tv.text = menuArray.getJSONObject(0).getString("name")
            detailMenu2Tv.text = menuArray.getJSONObject(1).getString("name")
            detailMenu3Tv.text = menuArray.getJSONObject(2).getString("name")

            detailMenuPrice1Tv.text = decimalFormat.format(menuArray.getJSONObject(0).getString("price").toInt())+"원"
            detailMenuPrice2Tv.text = decimalFormat.format(menuArray.getJSONObject(1).getString("price").toInt())+"원"
            detailMenuPrice3Tv.text = decimalFormat.format(menuArray.getJSONObject(2).getString("price").toInt())+"원"

            detailReviewCountTv.text = reviewCount.toString()               // 리뷰의 총 개수를 detailReviewCountTv View 에 표시한다.
            detailReviewGoodCountTv.text = "맛있다! ("+goodReviewCount.toString()+")"       // 맛있다 리뷰의 총 개수를 detailReviewGoodCountTv View 에 표시한다.
            detailReviewNormalCountTv.text = "괜찮다 ("+normalReviewCount.toString()+")"   // 괜찮다 리뷰의 총 개수를 detailReviewNormalCountTv View 에 표시한다.
            detailReviewBadCountTv.text = "별로 ("+badReviewCount.toString()+")"         // 별로 리뷰의 총 개수를 detailReviewBadCountTv View 에 표시한다.

            photoAdapter.setData(photoList)

            reviewAdapter.setData(reviewList)
            detailPb.visibility = View.GONE
        })
    }

    private fun setRecyclerViews(){
        // 사진 미리보기 RecyclerView 설정
        photoList = ArrayList()
        photoAdapter = PhotoAdapter(photoList, applicationContext, place.placeNo)
        val linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        detailPhotoRv.layoutManager = linearLayoutManager
        detailPhotoRv.adapter = photoAdapter


        // 댓글 RecyclerView 설정
        reviewList = ArrayList()
        reviewAdapter = ReviewAdapter(reviewList, applicationContext, place.placeNo)
        detailReviewRv.adapter = reviewAdapter
        detailReviewRv.layoutManager = LinearLayoutManager(applicationContext)
        detailReviewRv.isNestedScrollingEnabled = false // 내부 레이아웃에서 스크롤링이 스무스하게 되도록하는 설정

    }

    /*
     * 클릭 리스너에 뷰를 등록한다.
     */
    private fun setOnClickListeners(){
        detailCloseIv.setOnClickListener(this)
        detailWantToGoLo.setOnClickListener(this)
        detailWriteReviewLo.setOnClickListener(this)
        detailUploadPhotoLo.setOnClickListener(this)
        detailGoodReviewLo.setOnClickListener(this)
        detailNormalReviewLo.setOnClickListener(this)
        detailBadReviewLo.setOnClickListener(this)
        detailFindPathLo.setOnClickListener(this)
        detailNavigationLo.setOnClickListener(this)
        detailCallTaxiLo.setOnClickListener(this)
        detailAddrCopyLo.setOnClickListener(this)
        detailCallLo.setOnClickListener(this)
        detailMoreReviewTv.setOnClickListener(this)
        detailSearchBlogTv.setOnClickListener(this)
    }

    /*
     * 리뷰 쓰기 액티비티를 시작하는 메소드
     *
     * selectPoint 를 매개변수로 받는다.
     * 리뷰 쓰기 Activity 로 이동할 때 어떤 버튼을 누르느냐에 따라 selectPoint 값이 달라진다.
     * selectPoint 0 == 리뷰 쓰기, 5 == 맛있다, 3 == 보통 , 1 == 별로
     * WriteReviewActivity 로 전달된 selectPoint 값은 Emotion 초기화에 사용된다.
     */
    private fun startWriteReviewActivity(selectPoint : Int){
        val reviewIntent = Intent(this,WriteReviewActivity::class.java)
        reviewIntent.putExtra("selectPoint",selectPoint)
        reviewIntent.putExtra("placeNo",place.placeNo)
        startActivityForResult(reviewIntent,WRITE_CODE)
    }

    override fun onClick(v: View?) {
        when(v){
            detailCloseIv->onBackPressed() // 왼쪽 상단 닫기 버튼을 누르면 Activity 종료
            detailWantToGoLo->{ // 가고싶다 버튼
                // 새로운 가고싶다를 추가할 때
                if(!isFavorite){
                    // 채워진 별로 아이콘 변경
                    runOnUiThread({
                        Glide.with(applicationContext)
                                .load(getDrawable(R.drawable.ic_star_orange24dp))
                                .into(detailWantToGoIv)
                    })
                // 기존 가고싶다를 취소할 때
                }else{
                    // 라인만 있는 별로 아이콘 변경
                    runOnUiThread({
                        Glide.with(applicationContext)
                                .load(getDrawable(R.drawable.ic_star_orange_line_24dp))
                                .into(detailWantToGoIv)
                    })
                }
                isFavorite = !isFavorite // 가고 싶다 상태 변경
                detailWantToGoIv.startAnimation(scaleAnim) // 애니메이션 시작
                Thread{
                    val paramList = ArrayList<Params>()
                    paramList.add(Params("userEmail",userEmail))                // 유저 이메일
                    paramList.add(Params("isWant",isFavorite.toString()))       // 가고 싶다 상태
                    paramList.add(Params("placeNo",place.placeNo.toString()))   // 맛집 번호

                    HttpTask("wantToGo.php",paramList).execute()    // 가고 싶다 상태를 변경하는 php 로 전송한다.
                }.start()

            }
            detailWriteReviewLo->{ // 리뷰쓰기 버튼
               startWriteReviewActivity(0)
            }
            detailUploadPhotoLo->{ // 사진 올리기 버튼

            }
            detailGoodReviewLo->{ // 맛있다 리뷰 버튼
                startWriteReviewActivity(5)
            }
            detailNormalReviewLo->{ // 보통 리뷰 버튼
                startWriteReviewActivity(3)
            }
            detailBadReviewLo->{ // 별로 리뷰 버튼
                startWriteReviewActivity(1)
            }
            detailFindPathLo->{ // 길 찾기 버튼
                val selectPathFinder = SelectPathFinderBottomSheet.newInstance()
                selectPathFinder.setLatLng(myLatLng, LatLng(place.lat,place.lon)) // 나의 위치와 맛집의 위치를 설정한다.
                selectPathFinder.show(supportFragmentManager,"bottomSheet") // 바텀시트를 보여준다.
            }
            detailNavigationLo->{ // 네비게이션 버튼
                val mapBoxIntent = Intent(applicationContext, NavigationActivity::class.java)
                mapBoxIntent.putExtra("startLat",myLatLng.latitude)
                mapBoxIntent.putExtra("startLng",myLatLng.longitude)
                mapBoxIntent.putExtra("endLat",place.lat)
                mapBoxIntent.putExtra("endLng",place.lon)
                startActivity(mapBoxIntent)
            }
            detailCallTaxiLo->{ // 택시부르기 버튼

            }
            detailAddrCopyLo->{ // 주소 복사 버튼
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("label", address)
                clipboardManager.primaryClip = clipData
                Toast.makeText(applicationContext, "주소가 복사되었습니다.", Toast.LENGTH_SHORT).show()
            }
            detailCallLo->{ // 전화하기 버튼

            }
            detailMoreReviewTv->{ // 리뷰 더보기 버튼
                val moreReviewIntent = Intent(applicationContext,ReviewActivity::class.java)
                moreReviewIntent.putExtra("placeNo",place.placeNo)
                startActivity(moreReviewIntent)
            }
            detailSearchBlogTv->{ // 블로그 검색하기 버튼
                val naverIntent = Intent(applicationContext,SearchNaverActivity::class.java)
                naverIntent.putExtra("placeName",place.name)
                startActivity(naverIntent)
            }
        }
    }

    /*
     * WriteReviewActivity 에서 반환된 결과 값을 확인하는 메소드
     *
     * WriteReviewActivity 에서 리뷰가 정상적으로 작성이 되면 isRefresh 에 true 값이 넘어온다.
     * isRefresh 가 true 라면 맛집 정보를 서버에 다시 요청하게 된다.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                WRITE_CODE->{ // 요청 코드가 WRITE_CODE 라면
                    if(data!!.getBooleanExtra("isRefresh",false)){ // isRefresh 값을 가져온다.
                        getDetailInfo(place.placeNo) // 맛집 정보를 서버에 다시 요청한다.
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /*
     * 구글 지도가 준비된 다음 호출되는 메소드
     */
    override fun onMapReady(googleMap : GoogleMap?) {
        // 맛집 위치로 카메라 이동
        val latLng = LatLng(place.lat,place.lon)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,18.0f)
        googleMap!!.moveCamera(cameraUpdate)

        // 맛집 위치에 마커 생성
        googleMap.addMarker(MarkerOptions()
                .position(latLng)
                .title(place.name))
    }
}