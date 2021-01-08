// JS Goes here - ES6 supported

import "./css/main.css";

import $ from 'jquery';
import 'popper.js';
import 'bootstrap';

// $(document).ready(function() {
//   console.log('setting up scrollspy')
//   $('body').scrollspy({ target: '.scrollspy-navbar' });
// 
//   // Deactivate any, first, before we start up another
//   // $(window).off('activate.bs.scrollspy');
// 
//   $(window).on('activate.bs.scrollspy', function (_, obj) {
//     console.log('activate.bs.scrollspy');
//     console.log('obj:', obj);
//     console.log('obj.relatedTarget:', obj.relatedTarget);
//     var ele = $('.scrollspy-navbar a[href="' + obj.relatedTarget + '"]');
//     if(ele.length) {
//       var offset = ele.offset().left;
//       $('.scrollspy-navbar').animate({
//         scrollLeft: offset - 30
//       }, 300);
//     }
//   });
// 
//   $(window).on('scroll', function() {
//     var ele = $('.scrollspy-navbar .nav-link.active');
//     if(!ele.length) {
//       $('.scrollspy-navbar .nav-link:first-child').addClass('active');
//     }
//   })
// });