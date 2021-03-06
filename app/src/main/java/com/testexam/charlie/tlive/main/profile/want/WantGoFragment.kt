package com.testexam.charlie.tlive.main.profile.want


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.testexam.charlie.tlive.R
import com.testexam.charlie.tlive.common.HttpTask
import com.testexam.charlie.tlive.common.Params
import com.testexam.charlie.tlive.main.place.Place
import com.testexam.charlie.tlive.main.place.PlaceAdapter
import kotlinx.android.synthetic.main.fragment_want_to_go.*
import org.json.JSONArray

/**
 * 유저가 가고싶다 누른 맛집의 리스트를 보여주는 Fragment
 */
class WantGoFragment : Fragment() {
    private lateinit var wantList : ArrayList<Place>    // 가고싶은 맛집의 리스트
    private lateinit var wantAdapter : PlaceAdapter     // 맛집 어댑터

    private var userEmail = ""      // 현재 로그인한 유저의 이메일
    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_want_to_go, null)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // SharedPreference 에서 현재 로그인한 유저의 이메일을 가져온다.
        userEmail = context!!.getSharedPreferences("login", Context.MODE_PRIVATE).getString("email","none")
        setRecyclerView()   // RecyclerView 를 설정한다.

        // 스와이프 레이아웃에 리프레시 리스너를 설정한다.
        wantSwipeLo.setOnRefreshListener {
            getWantList()   // 스와이프 리프레시가 발생하면 유저가 가고싶다 누른 리스트를 다시 요청한다.
        }
    }

    /*
     * WantToGo RecyclerView 설정 하는 메소드
     */
    private fun setRecyclerView(){
        wantList = ArrayList()      // 가고싶은 리스트를 초기화한다.
        wantAdapter = PlaceAdapter(wantList,context!!)      // 맛집 어댑터를 초기화한다.

        val gridManager = GridLayoutManager(context!!,2)    // 그리드 레이아웃 매니저를 초기화한다.

        wantRv.adapter = wantAdapter
        wantRv.layoutManager = gridManager
        wantRv.isNestedScrollingEnabled = false

        getWantList()   // 가고싶은 맛집 리스트를 받아오는 메소드를 호출한다.
    }

    /*
     * 서버에서 가고싶은 맛집의 리스트를 받아오는 메소드
     */
    private fun getWantList(){
        Thread{
            try{
                val paramList = ArrayList<Params>() // 파라미터 ArrayList 를 초기화한다.
                paramList.add(Params("userEmail",userEmail))    // 파라미터에 userEmail 을 담는다.

                // result 에 서버에서 가고싶은 리스트를 받아와 넣는다.
                val result = HttpTask("getWantList.php",paramList).execute().get()
                if(result != "null"){   // 리스트가 null 이 아니라면
                    wantList.clear()    // 가고싶은 리스트를 초기화한다.
                    val wantJSONArray = JSONArray(result)   // JSONArray 로 파싱한다.
                    // JSONArray 길이 만큼 wantList 에 가고싶은 맛집의 데이터를 추가한다.
                    for(i in 0 until wantJSONArray.length()){
                        val wantObject = wantJSONArray.getJSONObject(i)
                        wantList.add(Place( // 가고싶은 맛집 리스트에 Place 객체를 추가한다.
                                (i+1),                                          // no
                                wantObject.getInt("placeNo"),                // placeNo
                                wantObject.getString("name"),                // name
                                wantObject.getDouble("lat"),                 // lat
                                wantObject.getDouble("lon"),                 // lon
                                wantObject.getString("nearStation"),         // nearStation
                                wantObject.getDouble("starScore").toFloat(), // starScore
                                wantObject.getString("category"),            // category
                                wantObject.getInt("viewNum"),                // viewNum
                                wantObject.getInt("reviewNum"),              // reviewNum
                                wantObject.getInt("distance"),               // distance
                                wantObject.getString("previewSrc")           // previewSrc
                        ))
                    }
                }
                activity!!.runOnUiThread({
                    wantAdapter.setPlaceList(wantList)       // 어댑터를 이용해 맛집 리스트를 갱신한다.
                    if(wantList.size > 0){
                        wantNoneTv.visibility = View.GONE
                    }else{
                        wantNoneTv.visibility = View.VISIBLE
                    }
                    if(wantSwipeLo != null){
                        wantSwipeLo.isRefreshing = false
                    }

                })
            }catch (e:Exception){
                e.printStackTrace()

            }
        }.start()
    }
}