import { createContext, useContext, useState } from "react";

const ContextApi = createContext();

export const ContextProvider = ({ children }) => {
    let getToken = null;
    try {
        const item = localStorage.getItem("JWT_TOKEN");
        if (item) {
            try {
                getToken = JSON.parse(item);
            } catch {
                getToken = item;
            }
        }
    } catch {
        getToken = null;
    }

    const [token, setToken] = useState(getToken);

    const sendData = {
        token,
        setToken,
    };

    return <ContextApi.Provider value={sendData}>{children}</ContextApi.Provider>
};


export const useStoreContext = () => {
    const context = useContext(ContextApi);
    return context;
}