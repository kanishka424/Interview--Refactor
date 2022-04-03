// console.log('Before');
// setTimeout(()=>{//asynchronus
//     console.log('reading from user');
// },1)
// console.log('After');


//JS is single threaded


// console.log('Before');//Before
// const user =getUser(1);
// console.log(user);//undefined
// console.log('After');//After

// function getUser(id){
//     setTimeout(()=>{
//         console.log('Reading from a user db.....');
//         return {id:id,gitHubUserName:'mosh'}
//     },2000)

// }

// JS data types 
// Boolean
// integer 
// String 
// null 
// undefined 
// Object 


// Three soluions for Asynchronus functions

// 1.Callback
// 2.Promises
// 3.Async/await



//CALLBACK


console.log('Before');//Before
const user =getUser(1,function(user){
    console.log('User',user);
});
//console.log(user)
console.log('After');//After

function getUser(id,callback){
    setTimeout(()=>{
        console.log('Reading a user from a database....');
        callback({id:id,gitHubUsername:'mosh'});////changed from previous code
    },2000);
    
}


// ******CALLBACK- AFUNCTION WE WOULD CALL WHEN A RESULT OF AN ASYNCHRONUS FUNCTION IS READY

