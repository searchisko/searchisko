# How to implement client side for Content rating feature!

## Abstract

This tutorial helps you to implement Searchisko's ["Personalized Content Rating"](http://docs.jbossorg.apiary.io/#personalizedcontentratingapi) feature into your JavaScript client application running in browser. 
This tutorial contains complete example implementation placed in [content_rating](content_rating) subfolder also.

Tutorial and example uses jQuery, but you are free to use JavaScript framework of your choice.

## Content

### Cross-Origin Resource Sharing (CORS) handling
To allow Cross-Origin calls to REST API, Searchisko contains full support of [Cross-Origin Resource Sharing (CORS)](http://www.w3.org/TR/cors/) specification.
But DON'T FORGET to set `withCredentials: true` into `XMLHttpRequest` JavaScript objects to make correct use of this method during calls necessary for Content Rating!

### Personalized Content Rating Implementation steps

#### 1. Show rating for document
Searchisko defines two [system fields](../rest_api/content/dcp_content_object.md) stored in each document, related to content rating:

* `sys_rating_avg` - Average rating of Document. It is updated automatically when "Personalized Content Rating API" is used. Contains float number value (with decimal positions) between 1 (worst) and 5 (best). Field is not present if nobody rated document yet.
* `sys_rating_num` - Number of users who rated this Document. It is updated automatically when "Personalized Content Rating API" is used. Contains positive integer number. Field is not present if nobody rated document yet. 

These fields are returned as part of content data from [Search REST API](http://docs.jbossorg.apiary.io/#searchapi), so you can use them directly for view. 

#### 2. Evaluate if user is logged in to show his last rating and allow him to rate

User have to be logged into Searchisko to show his last vote and allow him to vote again. 
To check login status you have to call [User Authentication Status API](http://docs.jbossorg.apiary.io/#userauthenticationstatusapi).
User authentication method depends on your Searchisko instance configuration, it uses some SSO service ideally (eg. sso.jboss.org in case of jboss.org Searchisko instance).

Call to `/auth/status` method returns you info whether user is authenticated or not. 
If user is authenticated, then you can call GET `/rating/?{id}` to get and show his latest rating. You can also allow user to rate content.


Example of `auth/status` call:
````
var searchiskoContentList = ... result of /search REST call with list of Searchisko documents we show
$.ajax({ 
	type : "GET",
	url : getApiUrlBase() + "auth/status",
	xhrFields:	{ withCredentials: true},
	success : function( data ) {
		if(data.authenticated){
			allowRating(searchiskoContentList);
			downloadPersonalizedRating(searchiskoContentList);
		} else {
			alert("user is not authenticated so can't make rating");
		}
	}
});
			
````

Example of GET `/rating/?{id}` call:
````
function downloadPersonalizedRating(searchiskoContentList){
	var ids = [];
	for (var i = 0; i < searchiskoContentList.hits.hits.length; i++) {
		ids.push(searchiskoContentList.hits.hits[i]._id);
	}
	$.ajax({ 
		type : "GET",
		url : getApiUrlBase() + "rating?id="+ids.join("&id="),
		xhrFields:	{ withCredentials: true},
		success : function( searchiskoRatingData ) {
			if(searchiskoRatingData){
			  showPersonalizedRating(searchiskoContentList, searchiskoRatingData);
			}
		}
	});				
}
````

#### 3. Post user's rating to the Searchisko

When authenticated user rates content, then you have to call POST `/rating/{id}` to pass new rating into Searchisko. 
This method return new average rating and numbers of rating for given document, so you can update view accordingly without other API call necessary.   

Example of POST `/rating/{id}` call:

````
function rate(id, rating){
	$.ajax({ 
		type : "POST",
		url : getApiUrlBase() + "rating/"+id,
		data : "{\"rating\":"+rating+"}",
		contentType: "application/json",
		xhrFields:	{ withCredentials: true},
		success : function( searchiskoRatingResponseData ) {
			if(searchiskoRatingResponseData){
				$("#"+id+" .rating_avg").html("Content rating: " + searchiskoRatingResponseData.sys_rating_avg);
				$("#"+id+" .rating_num").html("Number of votes: " + searchiskoRatingResponseData.sys_rating_num);
				$("#"+id+" .rating_your").html("Your rating: " + rating);
			}
		}
	});							
}
 ````